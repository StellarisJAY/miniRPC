package com.jay.rpc.service;

/**
 * <p>
 *  服务实现类扫描器接口
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 21:00
 */
public interface ServiceScanner {
    /**
     * 扫描
     * @param basePackage 扫描路径
     * @throws Exception e
     */
    void doScan(String basePackage) throws Exception;

    /**
     * get ServiceMapping
     * @return {@link ServiceMapping}
     */
    ServiceMapping getServiceMapping();

    /**
     * set ServiceMapping
     * @param serviceMapping {@link ServiceMapping}
     */
    void setServiceMapping(ServiceMapping serviceMapping);
}
