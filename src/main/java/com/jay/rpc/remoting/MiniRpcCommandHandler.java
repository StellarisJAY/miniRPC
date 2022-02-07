package com.jay.rpc.remoting;

import com.jay.dove.transport.command.AbstractCommandHandler;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.rpc.processor.RpcRequestProcessor;
import com.jay.rpc.service.ServiceMapping;

/**
 * <p>
 *  Command Handler
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 19:56
 */
public class MiniRpcCommandHandler extends AbstractCommandHandler {
    public MiniRpcCommandHandler(ServiceMapping serviceMapping, CommandFactory commandFactory) {
        super(commandFactory);
        this.registerProcessor(RpcProtocol.REQUEST, new RpcRequestProcessor(commandFactory, serviceMapping));
    }
}
