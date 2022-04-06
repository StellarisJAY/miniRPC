package com.jay.rpc.processor;

import com.jay.dove.compress.Compressor;
import com.jay.dove.compress.CompressorManager;
import com.jay.dove.serialize.Serializer;
import com.jay.dove.serialize.SerializerManager;
import com.jay.dove.transport.command.AbstractProcessor;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.dove.transport.command.RemotingCommand;
import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.entity.RpcResponse;
import com.jay.rpc.remoting.RpcProtocol;
import com.jay.rpc.remoting.RpcRemotingCommand;
import com.jay.rpc.service.LocalServiceCache;
import com.jay.rpc.service.ServiceInfo;
import com.jay.rpc.service.ServiceInstance;
import io.netty.channel.ChannelHandlerContext;
import io.prometheus.client.Gauge;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.zip.CRC32;

/**
 * <p>
 *  RPC 请求处理器
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 10:39
 */
@Slf4j
public class RpcRequestProcessor extends AbstractProcessor {
    /**
     * command工厂
     */
    private final CommandFactory commandFactory;

    private static final Gauge IN_PROGRESS_REQUESTS = Gauge.build()
            .name("rpc_in_progress_requests")
            .help("get in progress rpc requests count")
            .register();

    private static final Gauge TOTAL_REQUESTS = Gauge.build()
            .name("rpc_total_requests")
            .help("get total request count since start")
            .register();

    public RpcRequestProcessor(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    @Override
    public void process(ChannelHandlerContext channelHandlerContext, Object in) {
        if(in instanceof RpcRemotingCommand){
            IN_PROGRESS_REQUESTS.inc();
            TOTAL_REQUESTS.inc();
            RpcRemotingCommand command = (RpcRemotingCommand) in;
            RemotingCommand responseCommand = null;
            try{
                byte[] content = command.getContent();
                // CRC32 校验
                if(checkCrc32(content, command.getCrc32())){
                    // 获取压缩器编码
                    byte compressorCode = command.getCompressor();
                    if(compressorCode != -1){
                        // 解压数据部分
                        Compressor compressor = CompressorManager.getCompressor(compressorCode);
                        content = compressor.decompress(content);
                    }
                    // 获取序列化器
                    byte serializerCode = command.getSerializer();
                    Serializer serializer = SerializerManager.getSerializer(serializerCode);
                    // 反序列化
                    RpcRequest request = serializer.deserialize(content, RpcRequest.class);

                    // 处理请求
                    RpcResponse response = processRequest(request);
                    // 创建response
                    responseCommand = commandFactory.createResponse(command.getId(), response, RpcProtocol.RESPONSE);
                }else{
                    responseCommand = commandFactory.createExceptionResponse(command.getId(), "wrong crc32, network packet damaged");
                }
            } catch (Exception e) {
                log.error("request processing error: ", e);
                responseCommand = commandFactory.createExceptionResponse(command.getId(), e);
            }finally {
                // 发送response
                sendResponse(channelHandlerContext, responseCommand);
                IN_PROGRESS_REQUESTS.dec();
            }

        }
    }

    /**
     * 处理RPC request
     * @param request {@link RpcRequest}
     * @return {@link RpcResponse}
     * @throws Exception 无法找到目标服务实现类或者方法
     */
    private RpcResponse processRequest(RpcRequest request) throws Exception{
        // 从ServiceMapping获取实现类instance
        String serviceName = request.getServiceName();
        int version = request.getVersion();
        ServiceInfo serviceInfo = new ServiceInfo(request.getType(), version);
        ServiceInstance serviceInstance = LocalServiceCache.getServiceInstance(serviceInfo);

        // 没有找到实现类
        if(serviceInstance == null){
            throw new RuntimeException("can't find target service , service name: " + serviceName + ", version: " + version);
        }

        // 获取目标方法
        Class<?> clazz = serviceInstance.getClazz();
        Method method = clazz.getMethod(request.getMethodName(), request.getParameterTypes());
        RpcResponse.RpcResponseBuilder responseBuilder = RpcResponse.builder();
        try{
            // 设置方法访问权限
            method.setAccessible(true);
            // 反射执行方法
            Object result = method.invoke(serviceInstance.getInstance(), request.getParameters());
            responseBuilder.result(result);
        }catch (InvocationTargetException e){
            // 方法执行抛出异常
            log.error("invocation exception: ", e);
            responseBuilder.exception(e.getTargetException());
        }
        return responseBuilder.build();
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
        int value = (int)crc.getValue();
        return value == crc32;
    }
}
