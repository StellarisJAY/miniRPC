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
     * 入站过滤器
     */
    private static final Queue<Filter> INBOUND_FILTERS = new PriorityQueue<>((f1, f2)->f2.getPriority() - f1.getPriority());

    /**
     * 出站过滤器
     */
    private static final Queue<Filter> OUTBOUND_FILTERS = new PriorityQueue<>((f1,f2)->f2.getPriority() - f1.getPriority());

    /**
     * 添加过滤器
     * @param filter {@link Filter}
     */
    public static void addFilter(Filter filter){
        switch (filter.getDirection()){
            case INBOUND: INBOUND_FILTERS.offer(filter);break;
            case OUTBOUND: OUTBOUND_FILTERS.offer(filter);break;
            case BOTH: INBOUND_FILTERS.offer(filter);OUTBOUND_FILTERS.offer(filter);break;
            default:break;
        }
    }

    /**
     * 执行过滤器链
     * @param request {@link RpcRequest}
     * @return boolean
     */
    public static boolean executeFilterChain(RpcRequest request){
        for (Filter filter : INBOUND_FILTERS) {
            if(!filter.filter(request)){
                return false;
            }
        }
        return true;
    }

    public static boolean executeInboundFilters(RpcRequest request){
        for (Filter filter : INBOUND_FILTERS) {
            if(!filter.filter(request)){
                return false;
            }
        }
        return true;
    }

    public static boolean executeOutboundFilters(RpcRequest request){
        for (Filter filter : OUTBOUND_FILTERS) {
            if(!filter.filter(request)){
                return false;
            }
        }
        return true;
    }

    public static List<Filter> filters(){
        List<Filter> filters = new ArrayList<>(INBOUND_FILTERS.size() + OUTBOUND_FILTERS.size());
        filters.addAll(INBOUND_FILTERS);
        filters.addAll(OUTBOUND_FILTERS);
        return filters;
    }

}
