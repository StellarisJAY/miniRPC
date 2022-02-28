package com.jay.rpc.registry.redis;

import com.alibaba.fastjson.JSON;
import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.registry.LocalRegistry;
import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.registry.Registry;
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
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 14:36
 */
@Slf4j
public class RedisRegistry implements Registry {
    private JedisPool pool;
    private static final String KEY_PREFIX = "mini-rpc/";
    private static final String SUBSCRIBE_PATTERN = "*";

    /**
     * 本地注册中心缓存
     */
    private LocalRegistry localRegistry;

    /**
     * 订阅事件线程池
     */
    private final ExecutorService subscribeThreads = ThreadPoolUtil.newCachedThreadPool("redis-subscribe-");

    @Override
    public void init(){
        String host = MiniRpcConfigs.redisHost();
        int port = MiniRpcConfigs.redisPort();
        pool = new JedisPool(host, port);
        // 开启一个线程，订阅Redis注册中心的服务注册topic
        subscribeThreads.submit(()->{
            log.info("注册中心事件订阅已开启，keys：mini-rpc/*/providers");
            try(Jedis jedis = pool.getResource()){
                jedis.psubscribe(new JedisPubSub() {
                    @Override
                    public void onPMessage(String pattern, String channel, String message) {

                        if(localRegistry != null){
                            // 解析message JSON
                            ProviderNode node = JSON.parseObject(message, ProviderNode.class);
                            // 注册到本地注册中心
                            String[] parts = channel.split("/");
                            localRegistry.registerProvider(parts[1], node);
                        }
                    }
                }, KEY_PREFIX + "*/providers");
            }
        });
    }

    @Override
    public void setLocalRegistry(LocalRegistry localRegistry) {
        this.localRegistry = localRegistry;
    }

    @Override
    public Set<ProviderNode> lookupProviders(String groupName) {
        Set<ProviderNode> nodes = new HashSet<>();
        try(Jedis jedis = pool.getResource()){
            // hash get all 获取该group的所有provider
            Map<String, String> map = jedis.hgetAll(KEY_PREFIX + groupName + "/providers");
            // 解析JSON
            for(String s : map.values()){
                ProviderNode node = JSON.parseObject(s, ProviderNode.class);
                nodes.add(node);
            }
        }catch (Exception e){
            log.error("redis registry look up error: ", e);
        }
        return nodes;
    }

    @Override
    public void registerProvider(String groupName, ProviderNode node) {
        try(Jedis jedis = pool.getResource()){
            String json = JSON.toJSONString(node);
            // hash添加providerNode信息，key为url，value为JSON
            jedis.hset(KEY_PREFIX + groupName + "/providers", node.getUrl(), json);
            // 发布注册事件
            jedis.publish(KEY_PREFIX + groupName + "/providers", json);
        }catch (Exception e){
            log.error("register provider failed , ", e);
        }
    }
}
