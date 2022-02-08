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
import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.entity.RpcResponse;
import com.jay.rpc.loadbalance.LoadBalance;
import com.jay.rpc.loadbalance.RandomLoadBalance;
import com.jay.rpc.registry.LocalRegistry;
import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.registry.Registry;
import com.jay.rpc.registry.redis.RedisRegistry;
import com.jay.rpc.remoting.RpcCommandFactory;
import com.jay.rpc.remoting.RpcConnectionFactory;
import com.jay.rpc.remoting.RpcProtocol;
import com.jay.rpc.remoting.RpcRemotingCommand;
import com.jay.rpc.serialize.ProtostuffSerializer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.zip.CRC32;

/**
 * <p>
 *  RPC客户端
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 11:47
 */
@Slf4j
public class MiniRpcClient {
    /**
     * DOVE网络客户端
     */
    private final DoveClient client;
    /**
     * Command工厂
     */
    private final CommandFactory commandFactory;
    /**
     * 本地注册中心缓存
     */
    private LocalRegistry localRegistry;
    /**
     * 远程注册中心客户端
     */
    private Registry registry;
    /**
     * 负载均衡器
     */
    private LoadBalance loadBalance;

    private int maxConnections;


    public MiniRpcClient() {
        // 连接管理器
        ConnectionManager connectionManager = new ConnectionManager(new RpcConnectionFactory());
        this.commandFactory = new RpcCommandFactory();
        this.client = new DoveClient(connectionManager, commandFactory);
        // 注册协议
        ProtocolManager.registerProtocol(RpcProtocol.PROTOCOL_CODE, new RpcProtocol(new ClientSideCommandHandler(this.commandFactory)));
        // 注册序列化器
        SerializerManager.registerSerializer((byte)1, new ProtostuffSerializer());
        init();
    }

    private void init(){
        String registryType = MiniRpcConfigs.get("mini-rpc.registry.type");
        String loadBalanceType = MiniRpcConfigs.get("mini-rpc.client.load-balance");
        this.maxConnections = MiniRpcConfigs.getInt("mini-rpc.client.max-conn");
        if("redis".equals(registryType)){
            this.registry = new RedisRegistry();
        }
        // 初始化本地注册中心
        this.localRegistry = new LocalRegistry(registry);
        // 初始化远程注册中心客户端
        this.registry.init();
        this.registry.setLocalRegistry(localRegistry);

        // 初始化负载均衡器
        if("random".equals(loadBalanceType)){
            this.loadBalance = new RandomLoadBalance();
        }
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
        Url url = lookupProvider(producer);
        url.setExpectedConnectionCount(maxConnections);
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

    private Url lookupProvider(String groupName){
        // 本地缓存获取provider
        List<ProviderNode> providerNodes = localRegistry.lookUpProviders(groupName);
        ProviderNode provider = loadBalance.select(providerNodes);
        return Url.parseString(provider.getUrl());
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

    public void shutdown(){

    }
}
