package com.jay.rpc.entity;

import lombok.Builder;
import lombok.Getter;

/**
 * <p>
 *  RPC Response
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 10:52
 */
@Builder
@Getter
public class RpcResponse {
    /**
     * 函数返回值
     */
    private Object result;
    /**
     * 抛出异常
     */
    private Throwable exception;
}
