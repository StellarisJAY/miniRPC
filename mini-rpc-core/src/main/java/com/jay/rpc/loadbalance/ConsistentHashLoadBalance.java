package com.jay.rpc.loadbalance;

import com.google.common.hash.Hashing;
import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.registry.ProviderNode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * <p>
 *  一致性HASH负载均衡
 * </p>
 *
 * @author Jay
 * @date 2022/02/28 12:26
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance{

    /**
     * 一致性hash虚节点个数
     */
    public static final int V_NODE_COUNT = 10;

    @Override
    public ProviderNode doSelect(List<ProviderNode> providerNodes, RpcRequest request) {
        TreeMap<Integer, ProviderNode> ring = new TreeMap<>();
        for (ProviderNode providerNode : providerNodes) {
            String url = providerNode.getUrl();
            for (int i = 0; i < V_NODE_COUNT; i++) {
                int hash = hash(url + i);
                ring.put(hash, providerNode);
            }
        }
        // 用请求类名 + 版本号 + 方法名来求一致性hash
        String reqPath = request.getType().getName() + "/" + request.getVersion() + "/" + request.getMethodName();
        int hash = hash(reqPath);
        // 获取tailMap，如果tailMap是空的，表示环的下一个节点是头节点
        NavigableMap<Integer, ProviderNode> tailMap = ring.tailMap(hash, true);
        return tailMap.isEmpty() ? ring.firstEntry().getValue() : tailMap.firstEntry().getValue();
    }

    /**
     * 使用murmur算法来计算字符串的哈希值
     * @param src 源字符串
     * @return 32位int
     */
    @SuppressWarnings("all")
    private int hash(String src){
        return Hashing.murmur3_32().hashString(src, StandardCharsets.UTF_8).asInt();
    }
}
