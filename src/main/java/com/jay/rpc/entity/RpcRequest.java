package com.jay.rpc.entity;

import lombok.Builder;
import lombok.Getter;

/**
 * <p>
 *  RPC Request
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 10:50
 */
@Builder
@Getter
public class RpcRequest {
    /**
     * 服务名
     */
    private String serviceName;
    /**
     * 服务版本
     */
    private int version;

    /**
     * 方法名
     */
    private String methodName;
    /**
     * 参数类型
     */
    private Class<?>[] parameterTypes;
    /**
     * 参数列表
     */
    private Object[] parameters;
}
