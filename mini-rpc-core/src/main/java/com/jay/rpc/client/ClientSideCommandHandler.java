package com.jay.rpc.client;

import com.jay.dove.transport.command.AbstractCommandHandler;
import com.jay.dove.transport.command.CommandFactory;
import io.netty.channel.ChannelHandlerContext;

/**
 * <p>
 *  客户端CommandHandler
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 12:03
 */
public class ClientSideCommandHandler extends AbstractCommandHandler {
    public ClientSideCommandHandler(CommandFactory commandFactory) {
        super(commandFactory);
    }

    @Override
    public void channelInactive(ChannelHandlerContext channelHandlerContext) {

    }
}
