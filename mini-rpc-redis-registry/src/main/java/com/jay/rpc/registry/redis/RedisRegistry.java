package com.jay.rpc.registry.redis;

import com.alibaba.fastjson.JSON;
import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.registry.LocalRegistry;
import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.registry.Registry;
import com.jay.rpc.service.ServiceInfo;
import com.jay.rpc.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * <p>
 *  Redis 注册中心客户端
 *  注册原理：
 *  key: mini-rpc/services/{serviceName}/{version}
 *  value: hSet {providers}
 *
 *  获取服务provider：
 *  hGetAll: mini-rpc/services/{serviceName}/{version}
 *
 *  订阅服务信息更新：
 *  mini-rpc/services/*
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 14:36
 */
@Slf4j
public class RedisRegistry implements Registry {
    private JedisPool pool;
    private static final String KEY_PREFIX = "mini-rpc/services/";

    /**
     * 本地注册中心缓存
     */
    private LocalRegistry localRegistry;

    /**
     * 订阅事件线程池
     */
    private final ExecutorService subscribeThreads = ThreadPoolUtil.newCachedThreadPool("redis-subscribe-");

    @Override
    public Set<ProviderNode> lookupProviders(String serviceName, int version) {
        Set<ProviderNode> nodes = new HashSet<>();
        try (Jedis jedis = pool.getResource()) {
            Map<String, String> providers = jedis.hgetAll(KEY_PREFIX + serviceName + "/" + version);
            for (String json : providers.values()) {
                ProviderNode node = JSON.parseObject(json, ProviderNode.class);
                nodes.add(node);
            }
        } catch (Exception e) {
            log.error("Look up Providers Failed, service: {}-{}", serviceName, version, e);
        }
        return nodes;
    }

    @Override
    public void registerProvider(List<ServiceInfo> services, ProviderNode node) {
        String json = JSON.toJSONString(node);
        try (Jedis jedis = pool.getResource()) {
            for (ServiceInfo service : services) {
                String key = KEY_PREFIX + service.getType().getName() + "/" + service.getVersion();
                jedis.hset(key, node.getUrl(), json);
                jedis.publish(KEY_PREFIX + service.getType().getName() + "/" + service.getVersion(), json);
            }
            log.info("Provider Registered to Redis Registry");
        } catch (Exception e) {
            log.error("Register Provider Failed ", e);
        }
    }

    @Override
    public void init() {
        String host = MiniRpcConfigs.redisHost();
        int port = MiniRpcConfigs.redisPort();
        pool = new JedisPool(host, port);
        // 开启一个线程，订阅Redis注册中心的服务注册topic
        subscribeThreads.submit(() -> {
            log.info("注册中心事件订阅已开启，keys：mini-rpc/services/*");
            try (Jedis jedis = pool.getResource()) {
                jedis.psubscribe(new JedisPubSub() {
                    @Override
                    public void onPMessage(String pattern, String channel, String message) {
                        if (localRegistry != null) {
                            RedisRegistry.this.onMessage(channel, message);
                        }
                    }
                }, KEY_PREFIX + "*");
            }
        });
    }

    @Override
    public void heatBeat(List<ServiceInfo> services, ProviderNode node) {
        node.setLastHeartBeatTime(System.currentTimeMillis());
        registerProvider(services, node);
    }

    @Override
    public void setLocalRegistry(LocalRegistry localRegistry) {
        this.localRegistry = localRegistry;
    }

    private void onMessage(String channel, String message) {
        int idx = channel.indexOf(KEY_PREFIX);
        if (idx != -1) {
            String substring = channel.substring(idx + KEY_PREFIX.length() + 1);
            String[] content = substring.split("/");
            String serviceName = content[0];
            int version = Integer.parseInt(content[1]);
            ProviderNode node = JSON.parseObject(message, ProviderNode.class);
            localRegistry.registerProvider(serviceName, version, node);
        }
    }
}
