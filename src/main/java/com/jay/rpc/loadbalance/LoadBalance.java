package com.jay.rpc.loadbalance;

import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.spi.SPI;

import java.util.Set;

/**
 * <p>
 *  负载均衡接口
 * </p>
 *
 * @author Jay
 * @date 2022/02/08 13:19
 */
@SPI
public interface LoadBalance {

    /**
     * 选择provider
     * @param providerNodes provider集合
     * @return {@link ProviderNode}
     */
    ProviderNode select(Set<ProviderNode> providerNodes);
}
