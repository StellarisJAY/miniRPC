package com.jay.rpc.loadbalance;

import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.registry.ProviderNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  负载均衡抽象
 * </p>
 *
 * @author Jay
 * @date 2022/02/08 13:20
 */
public abstract class AbstractLoadBalance implements LoadBalance{
    @Override
    public ProviderNode select(Set<ProviderNode> providerNodes, RpcRequest request) {
        List<ProviderNode> list = new ArrayList<>(providerNodes);
        // 没有provider
        if(list.size() == 0){
            return null;
        }
        // 只有一个，直接返回
        if(list.size() == 1){
            return list.get(0);
        }else{
            // 执行负载均衡逻辑
            return doSelect(list, request);
        }
    }

    /**
     * 最终选择逻辑
     * @param providerNodes provider集合
     * @param request {@link RpcRequest}
     * @return {@link ProviderNode}
     */
    public abstract ProviderNode doSelect(List<ProviderNode> providerNodes, RpcRequest request);
}
