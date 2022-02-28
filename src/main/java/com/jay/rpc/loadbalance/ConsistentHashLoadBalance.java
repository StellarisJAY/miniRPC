package com.jay.rpc.loadbalance;

import com.jay.rpc.registry.ProviderNode;

import java.util.List;

/**
 * <p>
 *  一致性HASH负载均衡
 * </p>
 *
 * @author Jay
 * @date 2022/02/28 12:26
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance{
    @Override
    public ProviderNode doSelect(List<ProviderNode> providerNodes) {
        return null;
    }
}
