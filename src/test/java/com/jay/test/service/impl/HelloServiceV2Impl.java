package com.jay.test.service.impl;

import com.jay.rpc.annotation.RpcService;
import com.jay.test.service.HelloService;

/**
 * <p>
 *
 * </p>
 *
 * @author Jay
 * @date 2022/04/07 10:48
 */
@RpcService(type = HelloService.class, version = 2)
public class HelloServiceV2Impl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "hello v2 " + name;
    }
}
