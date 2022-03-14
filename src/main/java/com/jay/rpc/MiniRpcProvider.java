package com.jay.rpc;

import com.jay.dove.DoveClient;
import com.jay.dove.DoveServer;
import com.jay.dove.common.AbstractLifeCycle;
import com.jay.dove.config.DoveConfigs;
import com.jay.dove.serialize.SerializerManager;
import com.jay.dove.transport.codec.Codec;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.dove.transport.command.CommandHandler;
import com.jay.dove.transport.connection.ConnectionManager;
import com.jay.dove.transport.protocol.ProtocolManager;
import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.prometheus.PrometheusServer;
import com.jay.rpc.registry.LocalRegistry;
import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.registry.Registry;
import com.jay.rpc.registry.SimpleRegistry;
import com.jay.rpc.remoting.*;
import com.jay.rpc.serialize.ProtostuffSerializer;
import com.jay.rpc.service.ServiceInfo;
import com.jay.rpc.service.ServiceMapping;
import com.jay.rpc.spi.ExtensionLoader;
import com.jay.rpc.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;

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
    /**
     * dove client
     */
    private final DoveClient client;
    private final ServiceMapping serviceMapping;
    private final CommandHandler commandHandler;
    private final RpcProtocol rpcProtocol;
    private final LocalRegistry localRegistry;
    private final CommandFactory commandFactory;
    private final int port;

    private final PrometheusServer prometheusServer;

    public MiniRpcProvider(String basePackage) {
        // 获取服务器端口
        this.port = MiniRpcConfigs.serverPort();
        this.commandFactory = new RpcCommandFactory();
        Codec miniRpcCodec = new MiniRpcCodec();
        this.serviceMapping = new ServiceMapping(basePackage);
        // 创建客户端和连接管理器
        ConnectionManager connectionManager = new ConnectionManager(new RpcConnectionFactory());
        this.client = new DoveClient(connectionManager, commandFactory);
        // 创建本地注册中心缓存
        this.localRegistry = new LocalRegistry();
        this.commandHandler = new MiniRpcCommandHandler(serviceMapping, commandFactory, localRegistry);
        // dove服务器启用SSL
        if(MiniRpcConfigs.enableSsl()){
            DoveConfigs.setEnableSsl(true);
        }
        this.server = new DoveServer(miniRpcCodec, port, commandFactory);
        this.rpcProtocol = new RpcProtocol(commandHandler);
        this.prometheusServer = new PrometheusServer();
    }

    private void init() throws UnknownHostException {
        String registryType = MiniRpcConfigs.registryType();
        String serverHost = Inet4Address.getLocalHost().getHostAddress();
        // 生成节点信息
        ProviderNode node = ProviderNode.builder()
                .url(serverHost + ":" + port)
                .weight(10)
                .lastHeartBeatTime(System.currentTimeMillis())
                .build();
        // 初始化serviceMapping，扫描@RpcService实现类
        this.serviceMapping.init();
        // 创建注册中心客户端
        Registry registry;
        if(!MiniRpcConfigs.SIMPLE_REGISTRY.equalsIgnoreCase(registryType)){
            ExtensionLoader<Registry> registryLoader = ExtensionLoader.getExtensionLoader(Registry.class);
            registry = registryLoader.getExtension(registryType);
        }else{
            boolean isRegistry = MiniRpcConfigs.providerAsRegistry();
            registry = new SimpleRegistry(isRegistry, client, commandFactory);
        }

        this.localRegistry.setRemoteRegistry(registry);
        // 初始化远程注册中心
        registry.init();
        registry.setLocalRegistry(localRegistry);
        List<ServiceInfo> services = serviceMapping.listServices();
        // 注册当前provider
        registry.registerProvider(services, node);
        // 开启注册中心心跳
        registry.startHeartBeat(services, node);
    }

    @Override
    public void startup() {
        super.startup();
        long start = System.currentTimeMillis();
        try{
            init();
            // 注册协议
            ProtocolManager.registerProtocol(rpcProtocol.getCode(), rpcProtocol);
            // 注册序列化器
            SerializerManager.registerSerializer((byte)1, new ProtostuffSerializer());
            // 注册commandHandler线程池
            this.commandHandler.registerDefaultExecutor(ThreadPoolUtil.newIoThreadPool("rpc-command-handler-"));
            // 启动producer服务器
            this.server.startup();
            // 启动prometheus监控
            this.prometheusServer.startup();
            log.info("provider 启动完成，用时： {} ms", (System.currentTimeMillis() - start));
        }catch (Exception e){
            log.error("Provider 启动失败");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.server.shutdown();
    }
}
