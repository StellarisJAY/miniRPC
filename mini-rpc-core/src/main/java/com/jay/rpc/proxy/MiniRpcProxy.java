package com.jay.rpc.proxy;

import com.jay.dove.transport.Url;
import com.jay.rpc.callback.AsyncCallback;
import com.jay.rpc.client.MiniRpcClient;
import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.entity.RpcResponse;

import java.lang.reflect.Method;
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
     * @param version 服务版本
     * @return 代理对象
     */
    public static Object createInstance(Class<?> targetClass, int version){
        return Proxy.newProxyInstance(MiniRpcProxy.class.getClassLoader(), new Class[]{targetClass}, (proxy, method, args) -> {
            // 创建request
            RpcRequest request = RpcRequest.builder().methodName(method.getName())
                    .parameters(args)
                    .parameterTypes(method.getParameterTypes())
                    .version(version)
                    .type(targetClass)
                    .build();
            // 发送请求
            RpcResponse response = CLIENT.sendRequest(request);
            if(response.getException() != null){
                throw response.getException();
            }
            return response.getResult();
        });
    }

    /**
     * 创建指定目标地址的RPC代理类
     * @param targetClass {@link Class} 接口类
     * @param version 服务版本号
     * @param targetUrl 目标地址
     * @return 代理对象
     */
    public static Object createInstance(Class<?> targetClass, int version, Url targetUrl){
        return Proxy.newProxyInstance(MiniRpcProxy.class.getClassLoader(), new Class[]{targetClass}, (proxy, method, args) -> {
            // 创建request
            RpcRequest request = RpcRequest.builder().methodName(method.getName())
                    .parameters(args)
                    .parameterTypes(method.getParameterTypes())
                    .version(version)
                    .type(targetClass)
                    .build();
            RpcResponse response = CLIENT.sendRequest(request, targetUrl);
            if(response.getException() != null){
                throw response.getException();
            }
            return response.getResult();
        });
    }

    /**
     * 异步调用
     * @param targetClass 目标接口类
     * @param version 版本号
     * @param method 目标方法
     * @param callback {@link AsyncCallback}
     * @param args 参数列表
     */
    public static void asyncCall(Class<?> targetClass, int version, Method method, AsyncCallback callback, Object[] args){
        // 创建request
        RpcRequest request = RpcRequest.builder().methodName(method.getName())
                .parameters(args)
                .parameterTypes(method.getParameterTypes())
                .version(version)
                .type(targetClass)
                .build();
        CLIENT.sendRequestAsync(request, callback);
    }
}
