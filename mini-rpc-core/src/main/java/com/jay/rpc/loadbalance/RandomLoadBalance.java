package com.jay.rpc.loadbalance;

import com.jay.rpc.entity.RpcRequest;
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
    private final Random random = new Random();
    @Override
    public ProviderNode doSelect(List<ProviderNode> providerNodes, RpcRequest request) {
        int totalWeight = 0;
        boolean sameWeight = true;
        for(int i = 0; i < providerNodes.size(); i++){
            ProviderNode node = providerNodes.get(i);
            int weight = node.getWeight();
            totalWeight += weight;
            // 判断权重是否相同
            if(sameWeight && i > 0 && providerNodes.get(i - 1).getWeight() != weight){
                sameWeight = false;
            }
        }
        // provider的权重不同，使用区间法随机
        if(totalWeight > 0 && !sameWeight){
            int position = random.nextInt(totalWeight);
            for (ProviderNode node : providerNodes) {
                position -= node.getWeight();
                if (position < 0) {
                    return node;
                }
            }
        }
        // 权重相同，直接从列表随机
        return providerNodes.get(random.nextInt(providerNodes.size()));
    }
}
