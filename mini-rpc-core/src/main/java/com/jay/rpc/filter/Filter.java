package com.jay.rpc.filter;

import com.jay.rpc.entity.RpcRequest;

/**
 * <p>
 *
 * </p>
 *
 * @author Jay
 * @date 2022/04/06 14:33
 */
public interface Filter {

    /**
     * 初始化Filter
     */
    void init();
    /**
     * 过滤请求
     * @param request {@link RpcRequest}
     * @return 是否放行
     */
    boolean filter(RpcRequest request);

    /**
     * 获取filter的优先级
     * @return int
     */
    int getPriority();

    /**
     * 设置过滤器优先级
     * @param priority 优先级
     */
    void setPriority(int priority);

    /**
     * 添加排除的请求
     * @param request 格式：class/version/method
     */
    void addExclusion(String request);

    /**
     * 获取Filter方向
     * @return {@link FilterDirection}
     */
    FilterDirection getDirection();

    /**
     * 设置Filter方向
     * @param direction {@link FilterDirection}
     */
    void setDirection(FilterDirection direction);
}
