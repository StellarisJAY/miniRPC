package com.jay.rpc.starter;

import com.jay.dove.transport.Url;
import com.jay.rpc.annotation.RpcAutowired;
import com.jay.rpc.proxy.MiniRpcProxy;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

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
                String provider = annotation.provider();
                Object instance;
                if(StringUtil.isNullOrEmpty(provider)){
                    instance = MiniRpcProxy.createInstance(field.getType(), version);
                }else{
                    instance = MiniRpcProxy.createInstance(field.getType(), version, Url.parseString(provider));
                }
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
