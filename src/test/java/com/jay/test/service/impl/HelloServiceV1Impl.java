package com.jay.test.service.impl;

import com.jay.rpc.annotation.RpcService;
import com.jay.test.service.HelloService;

/**
 * <p>
 *
 * </p>
 *
 * @author Jay
 * @date 2022/04/07 10:47
 */
@RpcService(type = HelloService.class, version = 1)
public class HelloServiceV1Impl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "hello v1" + name;
    }
}
