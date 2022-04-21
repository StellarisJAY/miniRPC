package com.jay.rpc.remoting;

import com.jay.dove.transport.command.AbstractCommandHandler;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.rpc.processor.RpcRequestProcessor;
import com.jay.rpc.processor.SimpleRegistryProcessor;
import com.jay.rpc.registry.LocalRegistry;

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
        // Simple注册中心命令处理器
        this.registerProcessor(RpcProtocol.REGISTER, new SimpleRegistryProcessor(localRegistry, commandFactory));
        this.registerProcessor(RpcProtocol.LOOKUP, new SimpleRegistryProcessor(localRegistry, commandFactory));
    }
}
