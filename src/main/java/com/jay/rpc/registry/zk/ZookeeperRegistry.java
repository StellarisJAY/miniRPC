package com.jay.rpc.registry.zk;

import com.alibaba.fastjson.JSON;
import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.registry.LocalRegistry;
import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.registry.Registry;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.zookeeper.CreateMode;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  Zookeeper注册中心客户端
 * </p>
 *
 * @author Jay
 * @date 2022/02/09 11:20
 */
@Slf4j
public class ZookeeperRegistry implements Registry {
    /**
     * 本地注册中心缓存
     */
    private LocalRegistry localRegistry;
    /**
     * Zookeeper客户端
     */
    private CuratorFramework client;

    /**
     * Zookeeper 节点根路径
     */
    public static final String ROOT_PATH = "/mini-rpc/";

    @Override
    public Set<ProviderNode> lookupProviders(String groupName) {
        Set<ProviderNode> result = new HashSet<>();
        try{
            // ls 目录下的所有节点，路径：/mini-rpc/{groupName}/providers/
            List<String> children = client.getChildren().forPath(ROOT_PATH + groupName + "/providers/");
            // 获取每个节点信息
            for(String path : children){
                byte[] data = client.getData().forPath(path);
                String json = new String(data, StandardCharsets.UTF_8);
                // 反序列化JSON字符串
                ProviderNode node = JSON.parseObject(json, ProviderNode.class);
                result.add(node);
            }
        }catch (Exception e){
            log.error("look up provider error ", e);
        }
        return result;
    }

    @Override
    public void registerProvider(String groupName, ProviderNode node)  {
        try{
            /*
                路径：/mini-rpc/{groupName}/providers/{url}
                数据：JSON(ProviderNode)
                类型：-e
             */
            String path = client.create().creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(ROOT_PATH + groupName + "/providers/" + node.getUrl(),
                            JSON.toJSONString(node).getBytes(StandardCharsets.UTF_8));
        }catch (Exception e){
            log.error("register failed, cause: ", e);
        }
    }

    @Override
    public void init() {
        // 加载ZK options
        String host = MiniRpcConfigs.get("mini-rpc.registry.zookeeper.host");
        int port = MiniRpcConfigs.getInt("mini-rpc.registry.zookeeper.port");
        int sessionTimeout = MiniRpcConfigs.getInt("mini-rpc.registry.zookeeper.session-timeout");
        int connectionTimeout = MiniRpcConfigs.getInt("mini-rpc.registry.zookeeper.connection-timeout");
        long start = System.currentTimeMillis();
        // 创建curator客户端
        client = CuratorFrameworkFactory.newClient(host + ":" + port, sessionTimeout, connectionTimeout, new RetryOneTime(1000));
        // 启动客户端
        client.start();
        log.info("Zookeeper 注册中心客户端启动完成，用时：{}ms", (System.currentTimeMillis() - start));
    }

    @Override
    public void setLocalRegistry(LocalRegistry localRegistry) {
        this.localRegistry = localRegistry;
    }
}
