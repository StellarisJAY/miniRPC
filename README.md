# Mini-RPC

Mini-RPC 是 基于Netty开发的 使用TCP通信的 RPC框架。

Mini-RPC是SimpleRPC-NEW的升级版，使用自制的Dove框架开发，优化了底层的网络通信代码。不同于旧版本，Mini-RPC能够在没有Spring的情况下使用。

## 使用说明

### 概念说明

RPC系统中共有三种角色，注册中心、服务提供者（**Provider**）、服务消费者（**Consumer**）。

在Mini-RPC中，提供同一种服务的Provider的集合叫做服务组（**Group**），每个Provider中提供的服务实现类（**Service**）。

### maven依赖

```xml-dtd
<dependency>
	<groupId>com.jay</groupId>
    <artifactId>mini-rpc</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 配置文件

在项目Resources目录下创建mini-rpc.properties文件

#### 通用配置

```properties
# 注册中心类型
mini-rpc.registry.type = redis

# 注册中心地址
mini-rpc.registry.redis.host = 127.0.0.1
mini-rpc.registry.redis.port = 6379
```

#### Provider配置

```properties
# Provider 组
mini-rpc.provider.group = hello-group

# provider 服务器端口
mini-rpc.server.port = 8888
```

#### Consumer配置

```properties
# 客户端负载均衡算法
mini-rpc.client.load-balance = random
# 客户端最大连接数
mini-rpc.client.max-conn = 20
```

### 创建服务

使用@RpcService注解来声明一个服务类，Mini-RPC会在服务类扫描过程中通过注解识别。

```java
public interface HelloService{
    String sayHello(String name);
}

// 在注解的name属性输入服务名
@RpcService(name = "hello-service", version=1)
public class HelloServiceImplV1 implements HelloService{
    @Overrides
    public String sayHello(String name){
        return "hello v1 " + name;
    }
}

// 通过version属性来完成版本控制
@RpcService(name = "hello-service", version=2)
public class HelloServiceImplV2 implements HelloService{
    @Overrides
    public String sayHello(String name){
        return "hello v2 " + name;
    }
}
```



### 远程调用

使用MiniRpcProxy的createInstance方法创建RPC代理对象。方法参数列表如下：

- 服务接口类
- Provider所属group
- 服务名称
- 版本号

```java
HelloService serviceV1 = (HelloService)MiniRpcProxy.createInstance(HelloService.class, "hello-group", "hello-service", 1);
HelloService serviceV2 = (HelloService)MiniRpcProxy.createInstance(HelloService.class, "hello-group", "hello-service", 2);

log.info("v1: {}", serviceV1.sayHello("world"));
log.info("v2: {}", serviceV2.sayHello("world"));
```

