package com.jay.rpc.callback;

import com.jay.rpc.entity.RpcResponse;

/**
 * <p>
 *  异步调用Callback
 * </p>
 *
 * @author Jay
 * @date 2022/05/10 10:34
 */
public interface AsyncCallback {

    /**
     * 收到response
     * @param response {@link RpcResponse}
     */
    void onResponse(RpcResponse response);

    /**
     * 捕获到异常
     * @param throwable {@link Throwable}
     */
    void exceptionCaught(Throwable throwable);
}
