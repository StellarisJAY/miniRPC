package com.jay.rpc.client;

import com.jay.dove.DoveClient;
import com.jay.dove.compress.Compressor;
import com.jay.dove.compress.CompressorManager;
import com.jay.dove.config.Configs;
import com.jay.dove.serialize.Serializer;
import com.jay.dove.serialize.SerializerManager;
import com.jay.dove.transport.Url;
import com.jay.dove.transport.callback.InvokeFuture;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.dove.transport.command.RemotingCommand;
import com.jay.dove.transport.connection.ConnectionManager;
import com.jay.dove.transport.protocol.ProtocolManager;
import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.entity.RpcResponse;
import com.jay.rpc.remoting.*;
import com.jay.rpc.serialize.ProtostuffSerializer;

import java.util.zip.CRC32;

/**
 * <p>
 *  RPC客户端
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 11:47
 */
public class MiniRpcClient {
    /**
     * DOVE网络客户端
     */
    private final DoveClient client;
    /**
     * Command工厂
     */
    private final CommandFactory commandFactory;

    public MiniRpcClient() {
        // 连接管理器
        ConnectionManager connectionManager = new ConnectionManager(new RpcConnectionFactory());
        this.commandFactory = new RpcCommandFactory();
        this.client = new DoveClient(connectionManager, commandFactory);
        // 注册协议
        ProtocolManager.registerProtocol(RpcProtocol.PROTOCOL_CODE, new RpcProtocol(new ClientSideCommandHandler(this.commandFactory)));
        // 注册序列化器
        SerializerManager.registerSerializer((byte)1, new ProtostuffSerializer());
    }

    /**
     * 发送请求
     * @param producer 服务提供者名称
     * @param request 请求实体 {@link RpcRequest}
     * @return {@link RpcResponse}
     * @throws InterruptedException e
     */
    public RpcResponse sendRequest(String producer, RpcRequest request) throws InterruptedException {
        // 创建请求报文
        RemotingCommand requestCommand = commandFactory.createRequest(request, RpcProtocol.REQUEST);

        // 获取producer地址
        Url url = Url.parseString("127.0.0.1:9999?conn=10");
        // 发送请求，获得future
        InvokeFuture invokeFuture = client.sendFuture(url, requestCommand, null);
        // 等待结果
        RpcRemotingCommand responseCommand = (RpcRemotingCommand) invokeFuture.awaitResponse();

        if(responseCommand.getCommandCode().equals(RpcProtocol.RESPONSE)){
            // 获得response
            byte[] content = responseCommand.getContent();
            // 检查CRC32校验码
            if(checkCrc32(content, responseCommand.getCrc32())){
                // 解压数据部分
                byte compressorCode = responseCommand.getCompressor();
                if(compressorCode != -1){
                    Compressor compressor = CompressorManager.getCompressor(compressorCode);
                    content = compressor.decompress(content);
                }
                // 反序列化
                Serializer serializer = SerializerManager.getSerializer(responseCommand.getSerializer());
                return serializer.deserialize(content, RpcResponse.class);
            }
            else{
                // CRC32 错误，报文损坏
                throw new RuntimeException("network packet damaged during transport");
            }
        }else{
            // response 为 TIMEOUT 或 ERROR
            throw new RuntimeException(new String(responseCommand.getContent(), Configs.DEFAULT_CHARSET));
        }
    }

    /**
     * 检查content的CRC32校验值
     * @param content byte[]
     * @param crc32 crc32 value
     * @return true if crc32 correct
     */
    private boolean checkCrc32(byte[] content, int crc32){
        CRC32 crc = new CRC32();
        crc.reset();
        crc.update(content, 0, content.length);
        int value = (int) crc.getValue();
        return value == crc32;
    }
}
