package com.jay.rpc.loadbalance;

import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.registry.ProviderNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    public ProviderNode select(Set<ProviderNode> providerNodes) {
        // 过滤掉长期没有心跳的provider
        List<ProviderNode> list = new ArrayList<>(providerNodes);
        if(list.size() == 0){
            return null;
        }
        // 只有一个，直接返回
        if(list.size() == 1){
            return list.get(0);
        }else{
            // 执行负载均衡逻辑
            return doSelect(list);
        }
    }

    /**
     * 最终选择逻辑
     * @param providerNodes provider集合
     * @return {@link ProviderNode}
     */
    public abstract ProviderNode doSelect(List<ProviderNode> providerNodes);
}
