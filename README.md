# Mini-RPC

Mini-RPC 是 基于Netty开发的 使用TCP通信的 RPC框架。

Mini-RPC是SimpleRPC-NEW的升级版，使用自制的Dove框架开发，优化了底层的网络通信代码。不同于旧版本，Mini-RPC能够在没有Spring的情况下使用。

## 使用说明

### 概念说明

RPC系统中共有三种角色，注册中心、服务提供者（**Provider**）、服务消费者（**Consumer**）。

- **Provider**： 服务提供者，提供具体一种服务的服务器。
- **Group**：服务组，提供同一种服务的Provider组成的集群。消费者在调用服务时需要说明服务所在的组。
- **Service**：服务，在Mini-RPC中服务以 Java 类为载体，一个Java类就是一个服务。
- **Version**：服务版本，服务可以有不同的版本，因此同一个服务可以对应多个Java类。
- **Consumer**：服务消费者，通过RPC客户端调用远程服务。
- **Registry**：注册中心，消费者通过注册中心了解服务提供者的地址。Mini-RPC支持三种注册中心，Redis、Zookeeper和Simple。其中Simple不依赖外部应用，它会将指定的Provider服务器作为注册中心。

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
mini-rpc.registry.type = redis/zookeeper/simple

# 注册中心地址
# redis
mini-rpc.registry.redis.host = 127.0.0.1
mini-rpc.registry.redis.port = 6379
# zookeeper 
mini-rpc.registry.zookeeper.host = 127.0.0.1
mini-rpc.registry.zookeeper.port = 6379
# simple
mini-rpc.registry.simple.host = 127.0.0.1
mini-rpc.registry.simple.port = 6379
```

#### Provider配置

```properties
# Provider 组
mini-rpc.provider.group = hello-group

# provider 服务器端口
mini-rpc.server.port = 8888

# 是否将当前Provider作为注册中心，只有启用Simple注册中心时有效
mini-rpc.registry.provider-as-registry = true
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
// 调用 由 组hello-group中的服务器 提供的hello-service服务
HelloService serviceV1 = (HelloService)MiniRpcProxy.createInstance(HelloService.class, "hello-group", "hello-service", 1);
// 调用不同版本的服务
HelloService serviceV2 = (HelloService)MiniRpcProxy.createInstance(HelloService.class, "hello-group", "hello-service", 2);

log.info("v1: {}", serviceV1.sayHello("world"));
log.info("v2: {}", serviceV2.sayHello("world"));
```

