package com.jay.rpc;

import com.jay.dove.DoveServer;
import com.jay.dove.common.AbstractLifeCycle;
import com.jay.dove.serialize.SerializerManager;
import com.jay.dove.transport.codec.Codec;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.dove.transport.command.CommandHandler;
import com.jay.dove.transport.protocol.ProtocolManager;
import com.jay.rpc.remoting.MiniRpcCodec;
import com.jay.rpc.remoting.MiniRpcCommandHandler;
import com.jay.rpc.remoting.RpcCommandFactory;
import com.jay.rpc.remoting.RpcProtocol;
import com.jay.rpc.serialize.ProtostuffSerializer;
import com.jay.rpc.service.ServiceInfo;
import com.jay.rpc.service.ServiceMapping;
import com.jay.rpc.util.ThreadPoolUtil;

/**
 * <p>
 *  Mini-RPC 服务端
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 20:24
 */
public class MiniRpcProducer extends AbstractLifeCycle {
    /**
     * dove 服务器
     */
    private final DoveServer server;
    private final ServiceMapping serviceMapping;
    private final CommandHandler commandHandler;
    private final RpcProtocol rpcProtocol;

    public MiniRpcProducer(int port, String basePackage) {
        CommandFactory commandFactory = new RpcCommandFactory();
        Codec miniRpcCodec = new MiniRpcCodec();
        this.serviceMapping = new ServiceMapping(basePackage);
        this.commandHandler = new MiniRpcCommandHandler(serviceMapping, commandFactory);
        this.server = new DoveServer(miniRpcCodec, port, commandFactory);
        this.rpcProtocol = new RpcProtocol(commandHandler);
    }

    public Object getService(ServiceInfo serviceInfo){
        return serviceMapping.getServiceInstance(serviceInfo);
    }

    @Override
    public void startup() {
        super.startup();
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
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.server.shutdown();
    }
}
