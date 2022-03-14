package com.jay.rpc.registry.zk;

import com.alibaba.fastjson.JSON;
import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.registry.LocalRegistry;
import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.registry.Registry;
import com.jay.rpc.service.ServiceInfo;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.retry.RetryOneTime;
import org.apache.zookeeper.CreateMode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  Zookeeper注册中心客户端
 *  服务注册格式：
 *  path: /mini-rpc/services/{serviceName}/{version}/{url}
 *  data: JSON(ProviderNode)
 *
 *  订阅服务：
 *  watch: /mini-rpc/services
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
    public static final String ROOT_PATH = "/mini-rpc/services";

    @Override
    public Set<ProviderNode> lookupProviders(String serviceName, int version) {
        String path = getPath(serviceName, version);
        Set<ProviderNode> result = new HashSet<>();
        try {
            List<String> children = client.getChildren().forPath(path);
            for (String child : children) {
                String childPath = path + "/" + child;
                byte[] data = client.getData().forPath(childPath);
                String json = new String(data, MiniRpcConfigs.DEFAULT_CHARSET);
                ProviderNode node = JSON.parseObject(json, ProviderNode.class);
                result.add(node);
            }
        }catch (Exception e){
            log.error("Look up service error, service: {}_{}", serviceName, version, e);
        }
        return result;
    }

    @Override
    public void registerProvider(List<ServiceInfo> services, ProviderNode node) {
        String json = JSON.toJSONString(node);
        byte[] data = json.getBytes(MiniRpcConfigs.DEFAULT_CHARSET);
        String url = node.getUrl();
        for (ServiceInfo service : services) {
            try{
                client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
                        .forPath(getPath(service.getServiceName(), service.getVersion()) + "/" + url, data);
            }catch (Exception e){
                log.error("Register Provider Error, Service: {}_{}", service.getServiceName(), service.getVersion(), e);
            }
        }
    }

    @Override
    public void init() {
        // 加载ZK options
        String host = MiniRpcConfigs.zookeeperHost();
        int port = MiniRpcConfigs.zookeeperPort();
        int sessionTimeout = MiniRpcConfigs.ZOOKEEPER_SESSION_TIMEOUT;
        int connectionTimeout = MiniRpcConfigs.ZOOKEEPER_CONNECTION_TIMEOUT;
        long start = System.currentTimeMillis();
        // 创建curator客户端
        client = CuratorFrameworkFactory.newClient(host + ":" + port, sessionTimeout, connectionTimeout, new RetryOneTime(1000));
        // 启动客户端
        client.start();
        log.info("Zookeeper 注册中心客户端启动完成，用时：{}ms", (System.currentTimeMillis() - start));
        try{
            // 开启TreeCache，监听Node变化
            TreeCache treeCache = TreeCache.newBuilder(client, ROOT_PATH)
                    .setCacheData(true)
                    .build();
            treeCache.getListenable().addListener(new RegistryListener());
            treeCache.start();
        }catch (Exception e){
            log.error("Start TreeCache Failed ", e);
        }
    }

    @Override
    public void heatBeat(List<ServiceInfo> services, ProviderNode node) {

    }

    @Override
    public void setLocalRegistry(LocalRegistry localRegistry) {
        this.localRegistry = localRegistry;
    }


    class RegistryListener extends AbstractTreeCacheListener{

        @Override
        public void onNodeDataChanged(String path, byte[] data) {

        }

        @Override
        public void onNodeCreated(String path, byte[] data) {
            if(!StringUtil.isNullOrEmpty(path) && data != null && data.length > 0){
                ServiceInfo serviceInfo = getServiceInfo(path);
                if(serviceInfo != null){
                    // 注册新的Provider到本地Registry
                    String json = new String(data, MiniRpcConfigs.DEFAULT_CHARSET);
                    ProviderNode node = JSON.parseObject(json, ProviderNode.class);
                    localRegistry.registerProvider(serviceInfo.getServiceName(), serviceInfo.getVersion(), node);
                }
            }

        }

        @Override
        public void onNodeDeleted(String path)  {
            ServiceInfo serviceInfo = getServiceInfo(path);
            if(serviceInfo != null){
                // node被删除，Provider下线
                localRegistry.onProviderOffline(getUrl(path), serviceInfo.getServiceName(), serviceInfo.getVersion());
            }
        }
        private ServiceInfo getServiceInfo(String path){
            int idx = path.indexOf(ROOT_PATH);
            if(idx != -1) {
                String substring = path.substring(idx + ROOT_PATH.length() + 2);
                String[] parts = substring.split("/");
                String serviceName = parts[0];
                int version = Integer.parseInt(parts[1]);
                return new ServiceInfo(serviceName, version);
            }
            return null;
        }

        private String getUrl(String path){
            int i = path.lastIndexOf("/");
            return path.substring(i + 1);
        }
    }

    private String getPath(String serviceName, int version){
        return ROOT_PATH + "/" + serviceName + "/" + version;
    }

}
