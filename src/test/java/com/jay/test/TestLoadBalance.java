package com.jay.test;

import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.loadbalance.ConsistentHashLoadBalance;
import com.jay.rpc.loadbalance.LoadBalance;
import com.jay.rpc.loadbalance.RandomLoadBalance;
import com.jay.rpc.registry.ProviderNode;
import com.jay.test.service.HelloService;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 *  负载均衡单元测试
 * </p>
 *
 * @author Jay
 * @date 2022/04/07 10:43
 */
public class TestLoadBalance {

    @Test
    public void testRandomWithSameWeight(){
        LoadBalance loadBalance = new RandomLoadBalance();
        RpcRequest request = RpcRequest.builder().type(HelloService.class).version(1).methodName("sayHello").build();
        Set<ProviderNode> candidates = new HashSet<>();
        candidates.add(new ProviderNode("10.0.0.1:9000", 100, System.currentTimeMillis()));
        candidates.add(new ProviderNode("10.0.0.2:9000", 100, System.currentTimeMillis()));
        candidates.add(new ProviderNode("10.0.0.3:9000", 100, System.currentTimeMillis()));
        candidates.add(new ProviderNode("10.0.0.4:9000", 100, System.currentTimeMillis()));

        for(int i = 0 ; i < 10; i ++){
            System.out.println(loadBalance.select(candidates, request).getUrl());
        }
    }

    @Test
    public void testRandomWithWeight(){
        LoadBalance loadBalance = new RandomLoadBalance();
        RpcRequest request = RpcRequest.builder().type(HelloService.class).version(1).methodName("sayHello").build();
        Set<ProviderNode> candidates = new HashSet<>();
        candidates.add(new ProviderNode("10.0.0.1:9000", 100, System.currentTimeMillis()));
        candidates.add(new ProviderNode("10.0.0.2:9000", 200, System.currentTimeMillis()));
        candidates.add(new ProviderNode("10.0.0.3:9000", 100, System.currentTimeMillis()));
        candidates.add(new ProviderNode("10.0.0.4:9000", 400, System.currentTimeMillis()));

        for(int i = 0 ; i < 10; i ++){
            System.out.println(loadBalance.select(candidates, request).getUrl());
        }
    }

    @Test
    public void testConsistentHash(){
        LoadBalance loadBalance = new ConsistentHashLoadBalance();
        RpcRequest request = RpcRequest.builder().type(HelloService.class).version(1).methodName("sayHello").build();
        Set<ProviderNode> candidates = new HashSet<>();
        candidates.add(new ProviderNode("10.0.0.1:9000", 100, System.currentTimeMillis()));
        candidates.add(new ProviderNode("10.0.0.2:9000", 200, System.currentTimeMillis()));
        candidates.add(new ProviderNode("10.0.0.3:9000", 100, System.currentTimeMillis()));
        candidates.add(new ProviderNode("10.0.0.4:9000", 400, System.currentTimeMillis()));
        // 一致性哈希两次的结果应该一致
        ProviderNode firstSelect = loadBalance.select(candidates, request);
        ProviderNode secondSelect = loadBalance.select(candidates, request);
        Assert.assertEquals(firstSelect, secondSelect);
    }
}
