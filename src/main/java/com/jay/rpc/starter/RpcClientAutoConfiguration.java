package com.jay.rpc.starter;

import com.jay.rpc.annotation.RpcAutowired;
import com.jay.rpc.client.MiniRpcClient;
import com.jay.rpc.proxy.MiniRpcProxy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.lang.reflect.Field;

/**
 * <p>
 *
 * </p>
 *
 * @author Jay
 * @date 2022/04/06 10:33
 */
public class RpcClientAutoConfiguration implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Field[] fields = bean.getClass().getDeclaredFields();
        // 遍历bean的属性，找到有RpcAutowired注解的属性
        for (Field field : fields) {
            if(field.isAnnotationPresent(RpcAutowired.class)){
                RpcAutowired annotation = field.getAnnotation(RpcAutowired.class);
                int version = annotation.version();
                // 创建代理对象
                Object instance = MiniRpcProxy.createInstance(field.getType(), field.getType().getName(), version);
                // 重新设置field
                try{
                    field.setAccessible(true);
                    field.set(bean, instance);
                }catch (IllegalAccessException e){
                    e.printStackTrace();
                }
            }
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
