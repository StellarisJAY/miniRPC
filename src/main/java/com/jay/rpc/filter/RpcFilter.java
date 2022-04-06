package com.jay.rpc.filter;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *  RPC过滤器注解
 * </p>
 *
 * @author Jay
 * @date 2022/04/06 14:46
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface RpcFilter {
    /**
     * 排除的请求
     * @return String[] 每个请求的格式：类/版本/方法名
     */
    String[] exclusions();
}
