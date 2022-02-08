package com.jay.rpc.proxy;

import com.jay.rpc.client.MiniRpcClient;
import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.entity.RpcResponse;

import java.lang.reflect.Proxy;

/**
 * <p>
 *  RPC代理
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 11:39
 */
public class MiniRpcProxy {
    /**
     * RPC客户端
     */
    private static final MiniRpcClient CLIENT = new MiniRpcClient();

    /**
     * 创建代理对象
     * @param targetClass 目标接口
     * @param groupName producer名称
     * @param serviceName 服务名
     * @param version 服务版本
     * @return 代理对象
     */
    public static Object createInstance(Class<?> targetClass, String groupName, String serviceName, int version){
        return Proxy.newProxyInstance(MiniRpcProxy.class.getClassLoader(), new Class[]{targetClass}, (proxy, method, args) -> {
            // 创建request
            RpcRequest request = RpcRequest.builder().methodName(method.getName())
                    .parameters(args)
                    .parameterTypes(method.getParameterTypes())
                    .serviceName(serviceName)
                    .version(version)
                    .build();
            // 发送请求
            RpcResponse response = CLIENT.sendRequest(groupName, request);
            if(response.getException() != null){
                throw response.getException();
            }
            return response.getResult();
        });
    }

    public static void shutdown(){
        CLIENT.shutdown();
    }
}
