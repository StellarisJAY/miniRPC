package com.jay.rpc.processor;

import com.alibaba.fastjson.JSON;
import com.jay.dove.transport.command.AbstractProcessor;
import com.jay.dove.transport.command.CommandCode;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.rpc.registry.LocalRegistry;
import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.remoting.RpcProtocol;
import com.jay.rpc.remoting.RpcRemotingCommand;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * <p>
 *  Simple 注册中心请求处理器
 * </p>
 *
 * @author Jay
 * @date 2022/02/09 15:18
 */
public class SimpleRegistryProcessor extends AbstractProcessor {

    private final LocalRegistry localRegistry;
    private final CommandFactory commandFactory;
    public SimpleRegistryProcessor(LocalRegistry localRegistry, CommandFactory commandFactory) {
        this.localRegistry = localRegistry;
        this.commandFactory = commandFactory;
    }

    @Override
    public void process(ChannelHandlerContext channelHandlerContext, Object o) {
        if(o instanceof RpcRemotingCommand){
            RpcRemotingCommand request = (RpcRemotingCommand) o;
            CommandCode code = request.getCommandCode();
            if(RpcProtocol.REGISTER.equals(code)){
                processRegister(channelHandlerContext, request);
            }
            else{
                processLookup(channelHandlerContext, request);
            }
        }
    }

    private void processLookup(ChannelHandlerContext context, RpcRemotingCommand request){
        byte[] content = request.getContent();
        String groupName = new String(content, StandardCharsets.UTF_8);
        Set<ProviderNode> nodes = localRegistry.lookUpProviders(groupName);
        String json = JSON.toJSONString(nodes);
        RpcRemotingCommand response = (RpcRemotingCommand)commandFactory.createResponse(request.getId(), json, RpcProtocol.RESPONSE);
        sendResponse(context, response);
    }

    private void processRegister(ChannelHandlerContext context, RpcRemotingCommand request){
        byte[] content = request.getContent();
        // 解析请求JSON
        String json = new String(content, StandardCharsets.UTF_8);
        ProviderNode node = JSON.parseObject(json, ProviderNode.class);
        // 在本地注册中心注册Provider
        localRegistry.registerProvider(node.getGroupName(), node);
        // 发送response
        RpcRemotingCommand response = (RpcRemotingCommand)commandFactory.createResponse(request.getId(), "success", RpcProtocol.RESPONSE);
        sendResponse(context, response);
    }
}
