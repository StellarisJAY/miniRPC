package com.jay.rpc.registry.redis;

import com.alibaba.fastjson.JSON;
import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.registry.LocalRegistry;
import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.registry.Registry;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Redis 注册中心
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 14:36
 */
@Slf4j
public class RedisRegistry implements Registry {
    private JedisPool pool;
    private static final String KEY_PREFIX = "mini-rpc/";

    private LocalRegistry localRegistry;

    @Override
    public void init(){
        String host = MiniRpcConfigs.get("mini-rpc.registry.redis.host");
        int port = MiniRpcConfigs.getInt("mini-rpc.registry.redis.port");
        pool = new JedisPool(host, port);
    }

    @Override
    public void setLocalRegistry(LocalRegistry localRegistry) {
        this.localRegistry = localRegistry;
    }

    @Override
    public List<ProviderNode> lookupProviders(String groupName) {
        List<ProviderNode> nodes = new ArrayList<>();
        try(Jedis jedis = pool.getResource()){
            // hash get all 获取该group的所有provider
            Map<String, String> map = jedis.hgetAll(KEY_PREFIX + groupName + "/providers");
            // 解析JSON
            for(String s : map.values()){
                ProviderNode node = JSON.parseObject(s, ProviderNode.class);
                nodes.add(node);
            }
//            // 订阅 groupName下Provider注册
//            jedis.subscribe(new JedisPubSub() {
//                @Override
//                public void onMessage(String channel, String message) {
//                    // 解析注册的Provider
//                    ProviderNode node = JSON.parseObject(message, ProviderNode.class);
//                    // 添加到本地缓存
//                    localRegistry.registerProvider(groupName, node);
//                }
//            }, KEY_PREFIX + groupName + "/providers");
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
            // publish register事件
            jedis.publish(KEY_PREFIX + groupName + "/providers", json);
        }catch (Exception e){
            log.error("register provider failed , ", e);
        }
    }
}
