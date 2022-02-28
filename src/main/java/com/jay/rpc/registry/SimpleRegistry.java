package com.jay.rpc.registry;

import com.alibaba.fastjson.JSON;
import com.jay.dove.DoveClient;
import com.jay.dove.transport.Url;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.remoting.RpcProtocol;
import com.jay.rpc.remoting.RpcRemotingCommand;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 *  Simple注册中心
 *  将Provider作为注册中心
 * </p>
 *
 * @author Jay
 * @date 2022/02/09 14:56
 */
@Slf4j
public class SimpleRegistry implements Registry{
    /**
     * 当前服务器是否是注册中心
     */
    private final boolean isRegistry;
    /**
     * 本地注册中心缓存
     */
    private LocalRegistry localRegistry;
    /**
     * 远程注册中心客户端
     */
    private final DoveClient registryClient;
    private final CommandFactory commandFactory;

    private Url registryUrl;

    public SimpleRegistry(boolean isRegistry, DoveClient registryClient, CommandFactory commandFactory) {
        this.isRegistry = isRegistry;
        this.registryClient = registryClient;
        this.commandFactory = commandFactory;
    }

    @Override
    public void init() {
        if(!isRegistry){
            // 加载Simple注册中心地址
            String host = MiniRpcConfigs.simpleRegistryHost();
            int port = MiniRpcConfigs.simpleRegistryPort();
            this.registryUrl = Url.parseString(host + ":" + port);
        }
    }

    @Override
    public Set<ProviderNode> lookupProviders(String groupName) {
        Set<ProviderNode> nodes = new HashSet<>();
        if(!isRegistry){
            // 创建LOOK_UP请求
            RpcRemotingCommand request = (RpcRemotingCommand) commandFactory.createRequest(groupName, RpcProtocol.LOOKUP);
            try{
                // 发送LOOKUP请求
                RpcRemotingCommand response = (RpcRemotingCommand) registryClient.sendSync(registryUrl, request, null);
                byte[] content = response.getContent();
                // 解析JSON数据
                String json = new String(content, StandardCharsets.UTF_8);
                List<ProviderNode> nodeList = JSON.parseArray(json, ProviderNode.class);
                nodes.addAll(nodeList);
            }catch (Exception e){
                log.error("failed to look up providers from remote registry", e);
            }
        }
        return nodes;
    }

    @Override
    public void registerProvider(String groupName, ProviderNode node) {
        // 该Provider作为注册中心
        if(isRegistry){
            // 在本地注册
            localRegistry.registerProvider(groupName, node);
        }else{
            // 生成JSON
            String json = JSON.toJSONString(node);
            // 创建注册请求
            RpcRemotingCommand request = (RpcRemotingCommand) commandFactory.createRequest(json, RpcProtocol.REGISTER);
            try{
                // 发送注册请求
                RpcRemotingCommand response = (RpcRemotingCommand)registryClient.sendSync(registryUrl, request, null);
            }catch (Exception e){
                log.error("failed to register provider node, ", e);
            }
        }
    }



    @Override
    public void setLocalRegistry(LocalRegistry localRegistry) {
        this.localRegistry = localRegistry;
    }
}
