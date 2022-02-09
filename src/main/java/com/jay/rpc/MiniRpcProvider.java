package com.jay.rpc;

import com.jay.dove.DoveServer;
import com.jay.dove.common.AbstractLifeCycle;
import com.jay.dove.serialize.SerializerManager;
import com.jay.dove.transport.codec.Codec;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.dove.transport.command.CommandHandler;
import com.jay.dove.transport.protocol.ProtocolManager;
import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.registry.LocalRegistry;
import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.registry.Registry;
import com.jay.rpc.registry.redis.RedisRegistry;
import com.jay.rpc.remoting.MiniRpcCodec;
import com.jay.rpc.remoting.MiniRpcCommandHandler;
import com.jay.rpc.remoting.RpcCommandFactory;
import com.jay.rpc.remoting.RpcProtocol;
import com.jay.rpc.serialize.ProtostuffSerializer;
import com.jay.rpc.service.ServiceInfo;
import com.jay.rpc.service.ServiceMapping;
import com.jay.rpc.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 *  Mini-RPC 服务端
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 20:24
 */
@Slf4j
public class MiniRpcProvider extends AbstractLifeCycle {
    /**
     * dove 服务器
     */
    private final DoveServer server;
    private final ServiceMapping serviceMapping;
    private final CommandHandler commandHandler;
    private final RpcProtocol rpcProtocol;
    private Registry registry;
    private LocalRegistry localRegistry;
    private final int port;

    public MiniRpcProvider(String basePackage) {
        // 获取服务器端口
        this.port = MiniRpcConfigs.getInt("mini-rpc.server.port");
        CommandFactory commandFactory = new RpcCommandFactory();
        Codec miniRpcCodec = new MiniRpcCodec();
        this.serviceMapping = new ServiceMapping(basePackage);
        this.commandHandler = new MiniRpcCommandHandler(serviceMapping, commandFactory);
        this.server = new DoveServer(miniRpcCodec, port, commandFactory);
        this.rpcProtocol = new RpcProtocol(commandHandler);
    }

    private void init(){
        String registryType = MiniRpcConfigs.get("mini-rpc.registry.type");
        String groupName = MiniRpcConfigs.get("mini-rpc.provider.group");
        // 生成节点信息
        ProviderNode node = ProviderNode.builder()
                .url("127.0.0.1:" + port)
                .weight(10)
                .lastHeartBeatTime(System.currentTimeMillis())
                .build();
        // 根据类型创建注册中心
        if("redis".equals(registryType)){
            registry = new RedisRegistry();
        }
        // 创建本地注册中心缓存
        this.localRegistry = new LocalRegistry(registry);
        // 初始化远程注册中心
        registry.init();
        registry.setLocalRegistry(localRegistry);
        // 注册当前provider
        registry.registerProvider(groupName, node);
        log.info("provider注册完成, group: {}", groupName);
        // 开启注册中心心跳
        registry.startHeartBeat(groupName, node);
    }

    public Object getService(ServiceInfo serviceInfo){
        return serviceMapping.getServiceInstance(serviceInfo);
    }

    @Override
    public void startup() {
        super.startup();
        long start = System.currentTimeMillis();
        init();
        // 注册协议
        ProtocolManager.registerProtocol(rpcProtocol.getCode(), rpcProtocol);
        // 注册序列化器
        SerializerManager.registerSerializer((byte)1, new ProtostuffSerializer());
        // 注册commandHandler线程池
        this.commandHandler.registerDefaultExecutor(ThreadPoolUtil.newIoThreadPool("rpc-command-handler-"));
        // 初始化serviceMapping，扫描@RpcService实现类
        this.serviceMapping.init();
        // 启动producer服务器
        this.server.startup();
        log.info("provider 启动完成，用时： {} ms", (System.currentTimeMillis() - start));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.server.shutdown();
    }
}