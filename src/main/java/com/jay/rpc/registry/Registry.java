package com.jay.rpc.registry;

import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.service.ServiceInfo;
import com.jay.rpc.spi.SPI;
import com.jay.rpc.util.ThreadPoolUtil;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  注册中心接口
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 14:31
 */
@SPI
public interface Registry {
    /**
     * 从远程注册中心拉取该名称的生产者节点集合
     * @param serviceName 服务名
     * @param version 服务版本号
     * @return {@link List<ProviderNode>} 服务提供者集合
     */
    Set<ProviderNode> lookupProviders(String serviceName, int version);

    /**
     * 生产者节点通过该方法将自己注册到注册中心
     * @param services 服务集合
     * @param node Provider 信息
     */
    void registerProvider(List<ServiceInfo> services, ProviderNode node);

    /**
     * 初始化registry
     */
    void init();

    /**
     * 开启心跳
     * @param services 提供服务集合
     * @param node ProviderNode
     */
    default void startHeartBeat(List<ServiceInfo> services, ProviderNode node){
        ThreadPoolUtil.scheduleAtFixedRate(()->{
            node.setLastHeartBeatTime(System.currentTimeMillis());
            registerProvider(services, node);
        }, MiniRpcConfigs.REGISTER_TIMEOUT / 2, MiniRpcConfigs.REGISTER_TIMEOUT / 2, TimeUnit.MILLISECONDS);
    }

    /**
     * 设置本地注册中心
     * @param localRegistry {@link LocalRegistry}
     */
    void setLocalRegistry(LocalRegistry localRegistry);
}
