package com.jay.rpc.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 *
 * </p>
 *
 * @author Jay
 * @date 2022/03/22 13:35
 */
public class LocalServiceCache {
    private static final ConcurrentHashMap<ServiceInfo, ServiceInstance> INSTANCES = new ConcurrentHashMap<>();

    public static ServiceInstance getServiceInstance(ServiceInfo serviceInfo){
        return INSTANCES.get(serviceInfo);
    }

    public static void registerServiceInstance(ServiceInfo serviceInfo, ServiceInstance instance) throws Exception {
        if(INSTANCES.get(serviceInfo) != null){
            throw new Exception("register service failed, duplicate service info");
        }
        INSTANCES.put(serviceInfo, instance);
    }

    public static List<ServiceInfo> listServices(){
        return new ArrayList<>(INSTANCES.keySet());
    }
}
