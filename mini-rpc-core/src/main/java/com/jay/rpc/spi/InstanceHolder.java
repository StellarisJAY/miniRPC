package com.jay.rpc.spi;

/**
 * <p>
 *  Extension单例Holder
 * </p>
 *
 * @author Jay
 * @date 2022/02/28 10:23
 */
public class InstanceHolder <T>{
    /**
     * volatile实例
     */
    private volatile T instance;

    public T get(){
        return instance;
    }

    public void set(T value){
        this.instance = value;
    }
}
