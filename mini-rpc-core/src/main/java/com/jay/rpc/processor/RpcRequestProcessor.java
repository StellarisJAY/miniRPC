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
import com.jay.rpc.filter.FilterChain;
import com.jay.rpc.remoting.RpcProtocol;
import com.jay.rpc.remoting.RpcRemotingCommand;
import com.jay.rpc.service.LocalServiceCache;
import com.jay.rpc.service.ServiceInfo;
import com.jay.rpc.service.ServiceInstance;
import io.netty.channel.ChannelHandlerContext;
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

    public RpcRequestProcessor(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    @Override
    public void process(ChannelHandlerContext context, Object in) {
        RpcRemotingCommand command = (RpcRemotingCommand) in;
        RemotingCommand responseCommand;
        byte[] content = command.getContent();
        if(!checkCrc32(content, command.getCrc32())){
            // CRC32校验不通过，报文损坏
            responseCommand = commandFactory.createExceptionResponse(command.getId(), "wrong crc32, network packet damaged");
        }else{
            byte[] decompressedContent = decompressContent(command.getCompressor(), content);
            RpcRequest request = deserializeRequest(command.getSerializer(), decompressedContent);
            // 执行过滤器链
            if(FilterChain.executeInboundFilters(request)){
                // 处理请求
                RpcResponse response = processRequest(request);
                // 创建response
                responseCommand = commandFactory.createResponse(command.getId(), response, RpcProtocol.RESPONSE);
            }
            else{
                responseCommand = commandFactory.createResponse(command.getId(), "Request Blocked by Filter", RpcProtocol.ERROR);
            }
        }
        sendResponse(context, responseCommand);
    }



    /**
     * 解压content
     * @param compressorCode 压缩器编号
     * @param content 压缩内容
     * @return byte[]
     */
    private byte[] decompressContent(byte compressorCode, byte[] content){
        if(compressorCode != -1){
            // 解压数据部分
            Compressor compressor = CompressorManager.getCompressor(compressorCode);
            return compressor.decompress(content);
        }else{
            return content;
        }
    }

    /**
     * 反序列化请求
     * @param serializerCode 序列化器编号
     * @param content 序列化数据
     * @return {@link RpcRequest}
     */
    private RpcRequest deserializeRequest(byte serializerCode, byte[] content){
        Serializer serializer = SerializerManager.getSerializer(serializerCode);
        return serializer.deserialize(content, RpcRequest.class);
    }

    /**
     * 处理RPC request
     * @param request {@link RpcRequest}
     * @return {@link RpcResponse}
     */
    private RpcResponse processRequest(RpcRequest request){
        // 从ServiceMapping获取实现类instance
        Class<?> requestedClazz = request.getType();
        int version = request.getVersion();
        ServiceInfo serviceInfo = new ServiceInfo(requestedClazz, version);
        ServiceInstance serviceInstance = LocalServiceCache.getServiceInstance(serviceInfo);

        // 没有找到实现类
        if(serviceInstance == null){
            throw new RuntimeException("can't find target service , service name: " + requestedClazz + ", version: " + version);
        }
        RpcResponse.RpcResponseBuilder responseBuilder = RpcResponse.builder();
        try{
            // 获取目标方法
            Class<?> clazz = serviceInstance.getClazz();
            Method method = clazz.getMethod(request.getMethodName(), request.getParameterTypes());

            // 设置方法访问权限
            method.setAccessible(true);
            // 反射执行方法
            Object result = method.invoke(serviceInstance.getInstance(), request.getParameters());
            responseBuilder.result(result);
        }catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            log.error("Method invocation error ", e);
            responseBuilder.exception(e);
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
