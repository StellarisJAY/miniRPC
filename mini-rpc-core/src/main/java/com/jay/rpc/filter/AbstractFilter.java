package com.jay.rpc.filter;

import com.jay.rpc.entity.RpcRequest;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 *  过滤器抽象类
 * </p>
 *
 * @author Jay
 * @date 2022/04/06 14:33
 */
public abstract class AbstractFilter implements Filter{
    /**
     * exclude请求列表
     */
    private final Set<String> EXCLUSIONS = new HashSet<>();
    private int priority;
    private FilterDirection direction;

    public AbstractFilter(){

    }

    @Override
    public void init(){

    }

    @Override
    public final boolean filter(RpcRequest request) {
        Class<?> type = request.getType();
        int version = request.getVersion();
        String methodName = request.getMethodName();
        return EXCLUSIONS.contains(type.getName() + version + methodName) || doFilter(request);
    }

    /**
     * 过滤器过滤逻辑
     * @param request {@link RpcRequest}
     * @return boolean
     */
    public boolean doFilter(RpcRequest request){
        return true;
    }

    @Override
    public final int getPriority(){
        return priority;
    }

    @Override
    public void addExclusion(String exclusion){
        EXCLUSIONS.add(exclusion);
    }

    @Override
    public final void setPriority(int priority){
        this.priority = priority;
    }

    @Override
    public FilterDirection getDirection() {
        return direction;
    }

    @Override
    public final void setDirection(FilterDirection direction) {
        this.direction = direction;
    }
}
