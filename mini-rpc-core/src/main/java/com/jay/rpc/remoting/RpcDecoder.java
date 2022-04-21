package com.jay.rpc.remoting;

import com.jay.dove.transport.command.CommandCode;
import com.jay.dove.transport.protocol.ProtocolDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

/**
 * <p>
 *     MINI-RPC 解码器
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 19:52
 */
public class RpcDecoder implements ProtocolDecoder {
    @Override
    public void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) {
        // 标记解码起始位置，TCP拆包发生时回退
        in.markReaderIndex();
        // 检查协议编号
        byte protocolCode = in.readByte();
        if(protocolCode != RpcProtocol.PROTOCOL_CODE.value()){
            throw new RuntimeException("decoder error, wrong protocol");
        }
        // 检查可读数据长度，解决TCP拆包
        if(in.readableBytes() >= RpcProtocol.HEADER_LENGTH){
            // 读取报文头
            int length = in.readInt();
            short code = in.readShort();
            int id = in.readInt();
            long timeout = in.readLong();
            byte serializer = in.readByte();
            byte compressor = in.readByte();
            int crc32 = in.readInt();
            // 检查数据部分长度
            if(in.readableBytes() >= length - RpcProtocol.HEADER_LENGTH){
                // 读取数据部分
                byte[] content = new byte[length - RpcProtocol.HEADER_LENGTH];
                in.readBytes(content);
                // 构建完整报文
                RpcRemotingCommand command = RpcRemotingCommand.builder()
                        .length(length)
                        .id(id)
                        .commandCode(new CommandCode(code))
                        .timeout(timeout)
                        .serializer(serializer)
                        .compressor(compressor)
                        .crc32(crc32)
                        .content(content).build();
                out.add(command);
            }else{
                // 数据长度不足，发生TCP拆包，回退到解码开始处
                in.resetReaderIndex();
            }
        }else{
            // 数据长度不足解码首部，发生TCP拆包，回退到解码开始
            in.resetReaderIndex();
        }
    }
}
