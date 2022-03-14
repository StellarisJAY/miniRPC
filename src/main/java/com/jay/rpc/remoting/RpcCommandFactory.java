package com.jay.rpc.remoting;

import com.jay.dove.serialize.Serializer;
import com.jay.dove.serialize.SerializerManager;
import com.jay.dove.transport.command.CommandCode;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.dove.transport.command.RemotingCommand;
import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.entity.RpcResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

/**
 * <p>
 *  Command工厂
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 19:52
 */
public class RpcCommandFactory implements CommandFactory {

    public static final byte DEFAULT_SERIALIZER = (byte)1;
    public static final byte DEFAULT_COMPRESSOR = (byte)-1;
    public static final long DEFAULT_TIMEOUT = 10000;
    private final AtomicInteger idProvider = new AtomicInteger(0);

    @Override
    public RemotingCommand createRequest(Object data, CommandCode commandCode) {
        RpcRemotingCommand.RpcRemotingCommandBuilder builder = createHeader(idProvider.getAndIncrement(), commandCode);
        if(data instanceof RpcRequest){
            RpcRequest request = (RpcRequest) data;
            // 序列化request
            Serializer serializer = SerializerManager.getSerializer(DEFAULT_SERIALIZER);
            byte[] content = serializer.serialize(request, RpcRequest.class);
            // 计算length和crc32
            return builder.length(content.length + RpcProtocol.HEADER_LENGTH)
                    .crc32(crc32(content))
                    .content(content)
                    .build();
        }else if(data instanceof String){
            byte[] content = ((String) data).getBytes(StandardCharsets.UTF_8);
            return builder.length(content.length + RpcProtocol.HEADER_LENGTH)
                    .content(content)
                    .crc32(crc32(content))
                    .build();
        }
        return null;
    }

    @Override
    public <T> RemotingCommand createRequest(T t, CommandCode commandCode, Class<T> aClass) {
        RpcRemotingCommand.RpcRemotingCommandBuilder builder = createHeader(idProvider.getAndIncrement(), commandCode);
        Serializer serializer = SerializerManager.getSerializer(DEFAULT_SERIALIZER);
        byte[] content = serializer.serialize(t, aClass);

        return builder.content(content)
                .length(RpcProtocol.HEADER_LENGTH)
                .crc32(crc32(content))
                .build();
    }

    @Override
    public RemotingCommand createResponse(int id, Object data, CommandCode commandCode) {
        RpcRemotingCommand.RpcRemotingCommandBuilder builder = createHeader(id, commandCode);
        if(data instanceof RpcResponse){
            RpcResponse response = (RpcResponse) data;
            // 序列化
            Serializer serializer = SerializerManager.getSerializer(DEFAULT_SERIALIZER);
            byte[] content = serializer.serialize(response, RpcResponse.class);
            // 计算length和crc32
            return builder.length(content.length + RpcProtocol.HEADER_LENGTH)
                    .crc32(crc32(content))
                    .content(content)
                    .build();
        }
        else if(data instanceof String){
            byte[] content = ((String) data).getBytes(StandardCharsets.UTF_8);
            return builder.length(content.length + RpcProtocol.HEADER_LENGTH)
                    .crc32(crc32(content))
                    .content(content)
                    .build();
        }
        return null;
    }

    @Override
    public RemotingCommand createTimeoutResponse(int id, Object data) {
        if(data instanceof String){
            String message = (String) data;
            byte[] content = message.getBytes(StandardCharsets.UTF_8);
            return createHeader(id, RpcProtocol.TIMEOUT)
                    .content(content)
                    .length(RpcProtocol.HEADER_LENGTH + content.length)
                    .crc32(crc32(content))
                    .build();
        }

        return null;
    }

    @Override
    public RemotingCommand createExceptionResponse(int id, String message) {
        byte[] content = message.getBytes(StandardCharsets.UTF_8);
        return createHeader(id, RpcProtocol.ERROR)
                .content(content)
                .length(RpcProtocol.HEADER_LENGTH + content.length)
                .crc32(crc32(content))
                .build();
    }

    @Override
    public RemotingCommand createExceptionResponse(int id, Throwable exception) {
        String message = exception.getMessage();
        byte[] content = message.getBytes(StandardCharsets.UTF_8);
        return createHeader(id, RpcProtocol.ERROR)
                .content(content)
                .length(RpcProtocol.HEADER_LENGTH + content.length)
                .crc32(crc32(content))
                .build();
    }

    private int crc32(byte[] content){
        CRC32 crc32 = new CRC32();
        crc32.reset();
        crc32.update(content);
        return (int)crc32.getValue();
    }

    private RpcRemotingCommand.RpcRemotingCommandBuilder createHeader(int id, CommandCode code){
        return RpcRemotingCommand.builder()
                .id(id)
                .commandCode(code)
                .compressor(DEFAULT_COMPRESSOR)
                .serializer(DEFAULT_SERIALIZER)
                .timeout(System.currentTimeMillis() + DEFAULT_TIMEOUT);
    }
}
