package com.jay.rpc.metric.prometheus;

import com.jay.rpc.config.MiniRpcConfigs;
import io.prometheus.client.Collector;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 *  Prometheus监控HTTP服务器
 * </p>
 *
 * @author Jay
 * @date 2022/02/28 15:42
 */
@Slf4j
public class PrometheusServer {

    private HTTPServer httpServer;
    private final List<Collector> collectors = new LinkedList<>();

    /**
     * 添加Collector
     * @param collector {@link Collector}
     */
    public void addCollector(Collector collector){
        collectors.add(collector);
    }

    private void init() throws Exception{
        // 注册collector
        for (Collector collector : collectors) {
            collector.register();
        }
        // 初始化JVM信息Exporter
        DefaultExports.initialize();
        // 启动Prometheus HTTP服务器
        this.httpServer = new HTTPServer(MiniRpcConfigs.prometheusServerPort(), true);
    }

    public void startup() {
        try{
            init();
        }catch (Exception e){
            log.error("prometheus server error ", e);
        }
    }
    public void shutdown() {
        httpServer.stop();
    }
}
