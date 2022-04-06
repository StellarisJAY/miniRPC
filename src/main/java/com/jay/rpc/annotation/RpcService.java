package com.jay.rpc.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *  RPC服务实现类注解
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 20:36
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface RpcService {

    /**
     * 服务接口类
     * @return Class
     */
    Class<?> type();

    /**
     * 版本号
     * @return int
     */
    int version() default 0;
}
