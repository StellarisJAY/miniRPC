package com.jay.rpc.metric.prometheus.filter;

import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.filter.AbstractFilter;
import com.jay.rpc.filter.RpcFilter;
import com.jay.rpc.util.IpV4Util;
import io.prometheus.client.Gauge;

/**
 * <p>
 *  Prometheus指标收集过滤器
 * </p>
 *
 * @author Jay
 * @date 2022/04/27 15:35
 */
@RpcFilter(exclusions = "", priority = Integer.MAX_VALUE - 1)
public class PrometheusFilter extends AbstractFilter {

    private final Gauge incomingRequestGauge = Gauge.build()
            .name("in_request")
            .labelNames("provider", "serviceName", "version")
            .create();

    @Override
    public void init() {
        super.init();
        incomingRequestGauge.register();
    }

    @Override
    public boolean doFilter(RpcRequest request) {
        incomingRequestGauge.labels(IpV4Util.getIpV4Address(),
                getServiceName(request.getType(), request.getMethodName()),
                Integer.toString(request.getVersion()))
                .inc();
        return true;
    }

    private String getServiceName(Class<?> type, String methodName){
        return type.getName() + "." + methodName;
    }
}
