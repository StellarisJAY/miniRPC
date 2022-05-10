package com.jay.rpc.entity;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

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
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 请求服务接口类型
     */
    private Class<?> type;
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

    /**
     * openTracing context
     */
    private Map<String, String> traceContext;
}
