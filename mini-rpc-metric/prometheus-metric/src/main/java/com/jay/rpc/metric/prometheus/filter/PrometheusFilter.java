package com.jay.rpc.metric.prometheus.filter;

import com.jay.rpc.entity.RpcRequest;
import com.jay.rpc.filter.AbstractFilter;
import com.jay.rpc.filter.RpcFilter;
import io.prometheus.client.Gauge;

/**
 * <p>
 *
 * </p>
 *
 * @author Jay
 * @date 2022/04/27 15:35
 */
@RpcFilter(exclusions = "", priority = 200)
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
        incomingRequestGauge.labels("127.0.0.1:9999",
                getServiceName(request.getType(), request.getMethodName()),
                Integer.toString(request.getVersion()))
                .inc();
        return true;
    }

    private String getServiceName(Class<?> type, String methodName){
        return type.getName() + "." + methodName;
    }
}
