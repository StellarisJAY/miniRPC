package com.jay.rpc.loadbalance;

import com.jay.rpc.registry.ProviderNode;

import java.util.List;
import java.util.Random;

/**
 * <p>
 *     随机负载均衡
 * </p>
 *
 * @author Jay
 * @date 2022/02/08 13:25
 */
public class RandomLoadBalance extends AbstractLoadBalance{
    @Override
    public ProviderNode doSelect(List<ProviderNode> providerNodes) {
        Random random = new Random();
        return providerNodes.get(random.nextInt(providerNodes.size()));
    }
}
