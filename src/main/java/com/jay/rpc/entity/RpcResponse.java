package com.jay.rpc.entity;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

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
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 函数返回值
     */
    private Object result;
    /**
     * 抛出异常
     */
    private Throwable exception;
}
