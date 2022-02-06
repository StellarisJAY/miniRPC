package com.jay.rpc.remoting;

import com.jay.dove.transport.protocol.ProtocolEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * <p>
 *  MINI-RPC 编码器
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 19:52
 */
public class RpcEncoder implements ProtocolEncoder {
    @Override
    public void encode(ChannelHandlerContext context, Object in, ByteBuf out) {
        if(in instanceof RpcRemotingCommand){
            RpcRemotingCommand command = (RpcRemotingCommand) in;
            // 写入报文首部
            out.writeInt(command.getLength());
            out.writeShort(command.getCommandCode().value());
            out.writeInt(command.getId());
            out.writeLong(command.getTimeoutMillis());
            out.writeByte(command.getSerializer());
            out.writeByte(command.getCompressor());
            out.writeByte(command.getCrc32());
            // 写入数据部分
            out.writeBytes(command.getContent());
        }
    }
}
