package com.jay.rpc.registry;

import java.util.*;
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
    private final ConcurrentHashMap<String, Set<ProviderNode>> registryCache = new ConcurrentHashMap<>(256);

    /**
     * 远程注册中心客户端
     */
    private Registry remoteRegistry;

    public LocalRegistry() {
    }

    /**
     * 查询Provider
     * @param groupName group
     * @return {@link Set<ProviderNode>}
     */
    public Set<ProviderNode> lookUpProviders(String groupName){
        // 如果本地Registry缓存没有，就从远程Registry拉取
        registryCache.computeIfAbsent(groupName, (key)-> remoteRegistry.lookupProviders(groupName));
        return registryCache.get(groupName);
    }

    /**
     * 在本地注册中心注册Provider
     * @param groupName group
     * @param node {@link ProviderNode}
     */
    public void registerProvider(String groupName, ProviderNode node){
        // 创建Set
        registryCache.computeIfAbsent(groupName, k-> new HashSet<>());
        // 添加node
        Set<ProviderNode> providerNodes = registryCache.get(groupName);
        providerNodes.add(node);
    }

    public void setRemoteRegistry(Registry remoteRegistry){
        this.remoteRegistry = remoteRegistry;
    }

}
