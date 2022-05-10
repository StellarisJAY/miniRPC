package com.jay.rpc.remoting;

import com.jay.dove.transport.command.AbstractCommandHandler;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.rpc.processor.RpcRequestProcessor;
import com.jay.rpc.registry.LocalRegistry;
import io.netty.channel.ChannelHandlerContext;

/**
 * <p>
 *  Command Handler
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 19:56
 */
public class MiniRpcCommandHandler extends AbstractCommandHandler {
    public MiniRpcCommandHandler(CommandFactory commandFactory, LocalRegistry localRegistry) {
        super(commandFactory);
        // RPC 请求处理器
        this.registerProcessor(RpcProtocol.REQUEST, new RpcRequestProcessor(commandFactory));
    }

    @Override
    public void channelInactive(ChannelHandlerContext channelHandlerContext) {

    }
}
