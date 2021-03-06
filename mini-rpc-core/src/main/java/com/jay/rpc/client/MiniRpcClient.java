package com.jay.rpc.client;

import com.jay.dove.DoveClient;
import com.jay.dove.compress.Compressor;
import com.jay.dove.compress.CompressorManager;
import com.jay.dove.config.DoveConfigs;
import com.jay.dove.serialize.Serializer;
import com.jay.dove.serialize.SerializerManager;
import com.jay.dove.transport.Url;
import com.jay.dove.transport.callback.InvokeCallback;
import com.jay.dove.transport.callback.InvokeFuture;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.dove.transport.command.RemotingCommand;
import com.jay.dove.transport.connection.ConnectionManager;
import com.jay.dove.transport.protocol.ProtocolManager;
import com.jay.rpc.callback.AsyncCallback;
import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.entity.RpcResponse;
import com.jay.rpc.filter.FilterChain;
import com.jay.rpc.loadbalance.LoadBalance;
import com.jay.rpc.registry.LocalRegistry;
import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.registry.Registry;
import com.jay.rpc.remoting.RpcCommandFactory;
import com.jay.rpc.remoting.RpcConnectionFactory;
import com.jay.rpc.remoting.RpcProtocol;
import com.jay.rpc.remoting.RpcRemotingCommand;
import com.jay.rpc.serialize.ProtostuffSerializer;
import com.jay.rpc.spi.ExtensionLoader;
import com.jay.rpc.util.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
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
    private final LocalRegistry localRegistry;
    /**
     * 负载均衡器
     */
    private LoadBalance loadBalance;

    private int maxConnections;
    /**
     * 异步回调线程池
     */
    private final ExecutorService asyncExecutor = ThreadPoolUtil.newIoThreadPool("async-callback-");


    public MiniRpcClient() {
        // 启用SSL
        if(MiniRpcConfigs.enableSsl()){
            DoveConfigs.setEnableSsl(true);
        }
        // 连接管理器
        ConnectionManager connectionManager = new ConnectionManager(new RpcConnectionFactory());
        this.commandFactory = new RpcCommandFactory();
        this.localRegistry = new LocalRegistry();
        this.client = new DoveClient(connectionManager, commandFactory);
        // 注册协议
        ProtocolManager.registerProtocol(RpcProtocol.PROTOCOL_CODE, new RpcProtocol(new ClientSideCommandHandler(this.commandFactory, this.asyncExecutor)));
        // 注册序列化器
        SerializerManager.registerSerializer((byte)1, new ProtostuffSerializer());
        init();
    }

    private void init(){
        String registryType = MiniRpcConfigs.registryType();
        String loadBalanceType = MiniRpcConfigs.loadBalanceType();
        this.maxConnections = MiniRpcConfigs.maxConnections();
        if(!registryType.equalsIgnoreCase(MiniRpcConfigs.NONE_REGISTRY)){
            ExtensionLoader<Registry> registryLoader = ExtensionLoader.getExtensionLoader(Registry.class);
            /*
             * 远程注册中心客户端
             */
            Registry registry = registryLoader.getExtension(registryType);

            // 初始化本地注册中心
            this.localRegistry.setRemoteRegistry(registry);
            // 初始化远程注册中心客户端
            registry.init();
            registry.setLocalRegistry(localRegistry);
        }
        // 加载负载均衡器
        ExtensionLoader<LoadBalance> loadBalanceLoader = ExtensionLoader.getExtensionLoader(LoadBalance.class);
        this.loadBalance = loadBalanceLoader.getExtension(loadBalanceType);
    }

    /**
     * 发送请求
     * @param request 请求实体 {@link RpcRequest}
     * @return {@link RpcResponse}
     * @throws InterruptedException e
     */
    public RpcResponse sendRequest(RpcRequest request) throws InterruptedException {
        // 创建请求报文，command工厂序列化
        RemotingCommand requestCommand = commandFactory.createRequest(request, RpcProtocol.REQUEST, RpcRequest.class);

        // 获取producer地址
        Url url = lookupProvider(request);
        if(url == null){
            return RpcResponse.builder()
                    .exception(new NullPointerException("No provider found for " + request.getType().getName()))
                    .build();
        }
        if(!executeFilters(request)){
            return RpcResponse.builder().result(null)
                    .exception(new RuntimeException("Request blocked by outbound filters"))
                    .build();
        }
        url.setExpectedConnectionCount(maxConnections);
        try{
            // 发送请求，获得future
            InvokeFuture invokeFuture = client.sendFuture(url, requestCommand, null);
            // 等待结果
            RpcRemotingCommand responseCommand = (RpcRemotingCommand) invokeFuture.awaitResponse();
            return processResponse(responseCommand);
        }catch (ConnectException e) {
            return RpcResponse.builder()
                    .exception(new RuntimeException("Connect to target url failed, url: " + url.getOriginalUrl()))
                    .build();
        }

    }

    /**
     * 向指定地址发送RPC请求
     * @param request {@link RpcRequest}
     * @param url {@link Url} 目标地址
     * @return {@link RpcResponse}
     * @throws InterruptedException e
     */
    public RpcResponse sendRequest(RpcRequest request, Url url) throws InterruptedException {
        RemotingCommand command = commandFactory.createRequest(request, RpcProtocol.REQUEST, RpcRequest.class);
        url.setExpectedConnectionCount(MiniRpcConfigs.maxConnections());
        if(!executeFilters(request)){
            return RpcResponse.builder().result(null)
                    .exception(new RuntimeException("Request blocked by outbound filters"))
                    .build();
        }
        try{
            RpcRemotingCommand response = (RpcRemotingCommand) client.sendSync(url, command, null);
            return processResponse(response);
        }catch (ConnectException e){
            return RpcResponse.builder()
                    .exception(new RuntimeException("Connect to target url failed, url: " + url.getOriginalUrl()))
                    .build();
        }
    }

    /**
     * 发送异步请求
     * 同步发送，异步等待结果并调用callback处理
     * @param request {@link RpcRequest}
     * @param callback {@link AsyncCallback}
     */
    public void sendRequestAsync(RpcRequest request, AsyncCallback callback){
        // 创建请求报文，command工厂序列化
        RemotingCommand requestCommand = commandFactory.createRequest(request, RpcProtocol.REQUEST, RpcRequest.class);
        // 获取producer地址
        Url url = lookupProvider(request);
        if(url == null){
            callback.exceptionCaught(new NullPointerException("No provider server found for: " + request.getMethodName()));
        }else{
            url.setExpectedConnectionCount(maxConnections);
            if(!executeFilters(request)){
                callback.exceptionCaught(new RuntimeException("Request blocked by outbound filters"));
            }
            else{
                try{
                    // 发送请求，获得future
                    client.sendAsync(url, requestCommand, new RpcInvokeCallback(callback, null));
                }catch (ConnectException e){
                    callback.exceptionCaught(new RuntimeException("Connect to target url failed, url: " + url.getOriginalUrl()));
                }
            }
        }
    }

    public CompletableFuture<RpcResponse> sendFuture(RpcRequest request){
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        // 创建请求报文，command工厂序列化
        RemotingCommand requestCommand = commandFactory.createRequest(request, RpcProtocol.REQUEST, RpcRequest.class);
        // 获取producer地址
        Url url = lookupProvider(request);
        if(url == null){
            future.completeExceptionally(new NullPointerException("No provider server found for: " + request.getMethodName()));
        }else {
            url.setExpectedConnectionCount(maxConnections);
            if(!executeFilters(request)){
                future.completeExceptionally(new RuntimeException("Request blocked by outbound filters"));
                return future;
            }
            // 发送请求，获得future
            try {
                client.sendAsync(url, requestCommand, new RpcInvokeCallback(null, future));
            } catch (ConnectException e) {
                future.completeExceptionally(new RuntimeException("Connect to target url failed, url: " + url.getOriginalUrl()));
            }

        }
        return future;
    }


    /**
     * 处理收到的回复报文
     * 首先判断回复的命令类型，然后检查报文完整性
     * @param responseCommand {@link RpcRemotingCommand}
     * @return {@link RpcResponse}
     */
    private RpcResponse processResponse(RpcRemotingCommand responseCommand){
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
        }else if(responseCommand.getCommandCode().equals(RpcProtocol.ERROR)){
            // 服务端回复Error
            throw new RuntimeException(new String(responseCommand.getContent(), StandardCharsets.UTF_8));
        }else{
            // 请求Timeout
            throw new RuntimeException(new String(responseCommand.getContent(), StandardCharsets.UTF_8));
        }
    }

    private boolean executeFilters(RpcRequest request){
        return FilterChain.executeOutboundFilters(request);
    }

    /**
     * 查询服务提供者
     * @param request {@link RpcRequest}
     * @return 服务提供者地址
     */
    private Url lookupProvider(RpcRequest request){
        // 本地缓存获取provider
        Set<ProviderNode> providerNodes = localRegistry.lookUpProviders(request.getType().getName(), request.getVersion());
        if(providerNodes == null || providerNodes.isEmpty()){
            return null;
        }
        ProviderNode provider = loadBalance.select(providerNodes, request);
        return provider == null ? null : Url.parseString(provider.getUrl());
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

    class RpcInvokeCallback implements InvokeCallback {
        AsyncCallback callback;
        CompletableFuture<RpcResponse> future;

        RpcInvokeCallback(AsyncCallback callback, CompletableFuture<RpcResponse> future) {
            this.callback = callback;
            this.future = future;
        }
        @Override
        public void onComplete(RemotingCommand remotingCommand) {
            RpcResponse response = processResponse((RpcRemotingCommand) remotingCommand);
            Optional<CompletableFuture<RpcResponse>> optionalFuture = Optional.ofNullable(future);
            Optional<AsyncCallback> optionalCallback = Optional.ofNullable(callback);
            Throwable exception = response.getException();
            if(exception != null){
                optionalCallback.ifPresent(c->c.exceptionCaught(exception));
                optionalFuture.ifPresent(f->f.completeExceptionally(exception));
            }else{
                callback.onResponse(response);
                optionalFuture.ifPresent(f->f.complete(response));
            }
        }

        @Override
        public void exceptionCaught(Throwable throwable) {
            Optional<CompletableFuture<RpcResponse>> optionalFuture = Optional.ofNullable(future);
            Optional<AsyncCallback> optionalCallback = Optional.ofNullable(callback);
            optionalCallback.ifPresent(c->c.exceptionCaught(throwable));
            optionalFuture.ifPresent(f->f.completeExceptionally(throwable));
        }

        @Override
        public void onTimeout(RemotingCommand remotingCommand) {
            Optional<CompletableFuture<RpcResponse>> optionalFuture = Optional.ofNullable(future);
            Optional<AsyncCallback> optionalCallback = Optional.ofNullable(callback);
            TimeoutException e = new TimeoutException("Request timeout, request id: " + remotingCommand.getId());
            optionalCallback.ifPresent(c->c.exceptionCaught(e));
            optionalFuture.ifPresent(f->f.completeExceptionally(e));
        }

        @Override
        public ExecutorService getExecutor() {
            return asyncExecutor;
        }
    }
}
