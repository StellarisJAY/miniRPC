package com.jay.rpc.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 *  RPC服务实现映射
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 20:33
 */
public class ServiceMapping {
    private final String basePackage;
    private final ConcurrentHashMap<ServiceInfo, ServiceInstance> serviceMap = new ConcurrentHashMap<>(256);

    public ServiceMapping(String basePackage) {
        this.basePackage = basePackage;
    }

    public ServiceInstance getServiceInstance(ServiceInfo serviceInfo){
        return serviceMap.get(serviceInfo);
    }

    public void registerServiceInstance(ServiceInfo serviceInfo, ServiceInstance instance) throws Exception {
        if(serviceMap.get(serviceInfo) != null){
            throw new Exception("register service failed, duplicate service info");
        }
        serviceMap.put(serviceInfo, instance);
    }

    public void init(){
        try {
            ServiceScanner serviceScanner = new SimpleServiceScanner();
            serviceScanner.setServiceMapping(this);
            serviceScanner.doScan(basePackage.replace(".", "/"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ServiceInfo> listServices(){
        return new ArrayList<>(serviceMap.keySet());
    }
}
