package com.jay.rpc.loadbalance;

import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.registry.ProviderNode;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 *  轮询负载均衡
 * </p>
 *
 * @author Jay
 * @date 2022/02/28 12:26
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance{

    @Override
    public ProviderNode doSelect(List<ProviderNode> providerNodes, RpcRequest request) {
        return null;
    }
}
