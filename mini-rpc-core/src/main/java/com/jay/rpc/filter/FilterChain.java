package com.jay.rpc.filter;

import com.jay.rpc.entity.RpcRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * <p>
 *  过滤器执行链
 * </p>
 *
 * @author Jay
 * @date 2022/04/06 14:41
 */
public class FilterChain {
    /**
     * 过滤器排序集合，使用PriorityQueue排序
     */
    private static final Queue<Filter> FILTERS = new PriorityQueue<>((f1,f2)->f2.getPriority() - f1.getPriority());

    /**
     * 添加过滤器
     * @param filter {@link Filter}
     */
    public static void addFilter(Filter filter){
        FILTERS.offer(filter);
    }

    /**
     * 执行过滤器链
     * @param request {@link RpcRequest}
     * @return boolean
     */
    public static boolean executeFilterChain(RpcRequest request){
        for (Filter filter : FILTERS) {
            if(!filter.filter(request)){
                return false;
            }
        }
        return true;
    }

    public static List<Filter> filters(){
        return new ArrayList<>(FILTERS);
    }

}
