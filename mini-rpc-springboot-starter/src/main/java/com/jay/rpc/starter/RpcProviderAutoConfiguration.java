package com.jay.rpc.starter;

import com.jay.rpc.MiniRpcProvider;
import com.jay.rpc.annotation.RpcService;
import com.jay.rpc.filter.Filter;
import com.jay.rpc.filter.FilterChain;
import com.jay.rpc.filter.RpcFilter;
import com.jay.rpc.service.LocalServiceCache;
import com.jay.rpc.service.ServiceInfo;
import com.jay.rpc.service.ServiceInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * <p>
 *  Rpc服务提供者自动配置类
 * </p>
 *
 * @author Jay
 * @date 2022/04/06 10:21
 */
@Slf4j
public class RpcProviderAutoConfiguration implements InitializingBean, ApplicationContextAware {

    private ApplicationContext applicationContext;
    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(RpcService.class);
        if(beans.size() == 0){
            return;
        }
        MiniRpcProvider provider = new MiniRpcProvider();
        // 遍历beans
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object instance = entry.getValue();
            // 扫描RPCService注解
            if(instance.getClass().isAnnotationPresent(RpcService.class)){
                RpcService annotation = instance.getClass().getAnnotation(RpcService.class);
                Class<?> type = annotation.type();
                int version = annotation.version();
                // 封装serviceInfo，缓存在本地服务缓存
                ServiceInfo serviceInfo = new ServiceInfo(type, version);
                LocalServiceCache.registerServiceInstance(serviceInfo, new ServiceInstance(type, instance));
            }
            // 扫描RpcFilter注解
            else if(instance.getClass().isAnnotationPresent(RpcFilter.class)){
                RpcFilter annotation = instance.getClass().getAnnotation(RpcFilter.class);

                // 获取注解的exclusions属性
                String[] exclusions = annotation.exclusions();
                try{
                    Filter filter = (Filter) instance;
                    filter.setPriority(annotation.priority());
                    // 设置exclusions
                    for (String exclusion : exclusions) {
                        filter.addExclusion(exclusion);
                    }
                    filter.init();
                    FilterChain.addFilter(filter);
                }catch (Throwable e){
                    e.printStackTrace();
                }
            }

        }
        // 启动provider服务
        provider.startup();
        log.info("Mini RPC Provider started");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
