package com.jay.rpc.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.builder.InstanceBuilder;
import com.jay.rpc.config.MiniRpcConfigs;
import com.jay.rpc.registry.LocalRegistry;
import com.jay.rpc.registry.ProviderNode;
import com.jay.rpc.registry.Registry;
import com.jay.rpc.service.ServiceInfo;
import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *
 * </p>
 *
 * @author Jay
 * @date 2022/04/21 11:07
 */
@Slf4j
public class NacosRegistry implements Registry {

    private NamingService namingService;
    private LocalRegistry localRegistry;

    @Override
    public Set<ProviderNode> lookupProviders(String serviceName, int version) {
        try{
            List<Instance> instances = namingService.getAllInstances(serviceName + "/" + version, true);
            return instances.stream()
                    .map(instance -> ProviderNode.builder().url(instance.getIp() + ":" + instance.getPort())
                            .weight((int) instance.getWeight())
                            .build())
                    .collect(Collectors.toSet());
        }catch (Exception e){
            log.warn("Failed to lookup provider for: {}", serviceName + "/" + version, e);
        }
        return null;
    }

    @Override
    public void registerProvider(List<ServiceInfo> services, ProviderNode node) {
        try{
            String ip = Inet4Address.getLocalHost().getHostAddress();
            int port = MiniRpcConfigs.serverPort();
            for (ServiceInfo service : services) {
                Instance instance = InstanceBuilder.newBuilder()
                        .setIp(ip)
                        .setPort(port)
                        .setWeight((double) node.getWeight())
                        .setHealthy(true).build();
                namingService.registerInstance(service.getType().getName() + "/" + service.getVersion(), instance);
            }
        }catch (Exception e){
            throw new RuntimeException("Register provider failed ", e);
        }
    }

    @Override
    public void init() {
        try{
            Properties properties = new Properties();
            properties.setProperty("serverAddr", MiniRpcConfigs.nacosAddress());
            properties.setProperty("username", MiniRpcConfigs.nacosUser());
            properties.setProperty("password", MiniRpcConfigs.nacosPassword());
            log.info("Naming service properties: {}", properties);
            namingService = NamingFactory.createNamingService(properties);
        }catch (NacosException e){
            log.error("Create Nacos Naming service error ", e);
            throw new RuntimeException("Init nacos registry failed ", e);
        }
    }

    @Override
    public void heatBeat(List<ServiceInfo> services, ProviderNode node) {

    }

    @Override
    public void setLocalRegistry(LocalRegistry localRegistry) {
        this.localRegistry = localRegistry;
    }
}
