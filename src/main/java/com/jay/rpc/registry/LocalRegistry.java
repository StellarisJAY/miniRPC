package com.jay.rpc.registry;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 *  本地注册中心缓存
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 14:28
 */
public class LocalRegistry {
    /**
     * 客户端本地注册中心缓存
     */
    private final ConcurrentHashMap<String, List<ProviderNode>> registryCache = new ConcurrentHashMap<>(256);

    private final Registry remoteRegistry;

    public LocalRegistry(Registry remoteRegistry) {
        this.remoteRegistry = remoteRegistry;
    }

    public List<ProviderNode> lookUpProviders(String groupName){
        // 如果本地Registry缓存没有，就从远程Registry拉取
        registryCache.computeIfAbsent(groupName, (key)-> remoteRegistry.lookupProviders(groupName));
        return registryCache.get(groupName);
    }

    public void registerProvider(String groupName, ProviderNode node){
        registryCache.computeIfPresent(groupName, (k, v)->{
            v.add(node);
            return v;
        });
    }

}
