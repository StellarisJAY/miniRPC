package com.jay.rpc.registry.zk;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;

/**
 * <p>
 *  TreeCache Listener
 * </p>
 *
 * @author Jay
 * @date 2022/03/14 15:13
 */
@Slf4j
public abstract class AbstractTreeCacheListener implements TreeCacheListener {
    @Override
    public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {
        if(treeCacheEvent.getData() != null){
            String path = treeCacheEvent.getData().getPath();
            byte[] data = treeCacheEvent.getData().getData();
            switch (treeCacheEvent.getType()){
                case NODE_ADDED: onNodeCreated(path, data);break;
                case NODE_REMOVED:onNodeDeleted(path);break;
                case NODE_UPDATED:onNodeDataChanged(path, data);break;
                default:break;
            }
        }
    }

    /**
     * 节点数据更新
     * @param path path
     * @param data data
     * @throws Exception e
     */
    public abstract void onNodeDataChanged(String path, byte[] data) throws Exception;

    /**
     * 节点创建
     * @param path path
     * @param data data
     * @throws Exception e
     */
    public abstract void onNodeCreated(String path, byte[] data) throws Exception;

    /**
     * 节点删除
     * @param path path
     * @throws Exception e
     */
    public abstract void onNodeDeleted(String path) throws Exception;
}
