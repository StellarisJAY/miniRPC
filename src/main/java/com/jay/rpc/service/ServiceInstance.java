package com.jay.rpc.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 *  服务实现instance
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 11:05
 */
@Getter
@AllArgsConstructor
public class ServiceInstance {
    /**
     * class
     */
    private Class<?> clazz;
    /**
     * instance
     */
    private Object instance;
}
