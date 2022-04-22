package com.jay.rpc.registry;

import io.netty.util.internal.StringUtil;

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
     * @param serviceName 服务名称
     * @param version 服务版本号
     * @return {@link Set<ProviderNode>}
     */
    public Set<ProviderNode> lookUpProviders(String serviceName, int version){
        // 如果本地Registry缓存没有，就从远程Registry拉取
        registryCache.computeIfAbsent(serviceName + "-" + version, (key)-> {
            if(remoteRegistry != null){
                return remoteRegistry.lookupProviders(serviceName, version);
            }
            return null;
        });
        return registryCache.get(serviceName + "-" + version);
    }

    /**
     * 在本地注册中心注册Provider
     * @param serviceName 服务名称
     * @param version 服务版本号
     * @param node {@link ProviderNode}
     */
    public void registerProvider(String serviceName, int version, ProviderNode node){
        // 创建Set
        registryCache.computeIfAbsent(serviceName + "-" + version, k-> new HashSet<>());
        // 添加node
        Set<ProviderNode> providerNodes = registryCache.get(serviceName + "-" + version);
        providerNodes.add(node);
    }

    public void setRemoteRegistry(Registry remoteRegistry){
        this.remoteRegistry = remoteRegistry;
    }

    /**
     * 本地注册中心标记Provider下线
     * @param url Provider Url
     * @param serviceName 下线serviceNam
     * @param version 下线服务版本
     */
    public void onProviderOffline(String url, String serviceName, int version){
        if(!StringUtil.isNullOrEmpty(url) && !StringUtil.isNullOrEmpty(serviceName)){
            String service = serviceName + "-" + version;
            registryCache.computeIfPresent(service, (k,v)->{
                ProviderNode node = ProviderNode.builder().url(url).build();
                v.remove(node);
                return v;
            });
        }
    }

    public void updateProviderList(String serviceName, Set<ProviderNode> providerNodes){
        registryCache.put(serviceName, providerNodes);
    }
}
