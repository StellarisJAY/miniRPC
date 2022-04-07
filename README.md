# Mini-RPC

Mini-RPC 是 基于Netty开发的 使用TCP通信的 RPC框架。提供了多种注册中心、服务版本控制、客户端负载均衡、服务端过滤器等功能。

Mini-RPC提供了SpringBoot-Starter以及各种注解，可以很方便地在SpringBoot项目中使用。

## 使用说明

### 概念说明

RPC系统中共有三种角色，注册中心、服务提供者（**Provider**）、服务消费者（**Consumer**）。

- **Provider**： 服务提供者，提供具体一种服务的服务器。
- **Service**：服务，在Mini-RPC中服务以 Java 类为载体，一个Java类就是一个服务。
- **Version**：服务版本，服务可以有不同的版本，因此同一个服务可以对应多个Java类。
- **Consumer**：服务消费者，通过RPC客户端调用远程服务。
- **Registry**：注册中心，消费者通过注册中心了解服务提供者的地址。Mini-RPC支持Zookeeper和Redis作为注册中心。

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
@RpcService(type = HelloService.class, version=1)
public class HelloServiceImplV1 implements HelloService{
    @Overrides
    public String sayHello(String name){
        return "hello v1 " + name;
    }
}

// 通过version属性来完成版本控制
@RpcService(type=HelloService.class, version=2)
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
- 服务名称
- 版本号

```java
public class Consumer {
    @Test
    public void test(){
        // 调用 由 组hello-group中的服务器 提供的hello-service服务
		HelloService serviceV1 = (HelloService)MiniRpcProxy.createInstance(HelloService.class, "hello-service", 1);
		// 调用不同版本的服务
		HelloService serviceV2 = (HelloService)MiniRpcProxy.createInstance(HelloService.class, "hello-service", 2);

		log.info("v1: {}", serviceV1.sayHello("world"));
		log.info("v2: {}", serviceV2.sayHello("world"));
    }
}
```

### @RpcAutowired注解

使用RpcAutowired注解可以借助Spring容器来加载一个RPC代理对象，具体的用法如下：

```java
@RestController
public class TestController {
    // 在注解中指定调用服务的版本
    @RpcAutowired(version = 1)
    private HelloService helloService;

    @GetMapping("/test/v1/{name}")
    public String testHelloV1(@PathVariable("name") String name){
        return helloService.hello(name);
    }

```

### 过滤器

通过配置过滤器可以实现对请求的筛选过滤，过滤器可以配置exlusions来排除请求，也可以配置优先级来调节过滤器在执行链中的位置。

```java
// 通过注解配置排除的请求（请求类名/版本号/方法名）和优先级（值大优先）
@RpcFilter(exclusions = "com.jay.service.HelloService/1/sayHello", priority = 500)
public class MyFilter extends AbstractFilter {
    @Override
    public boolean doFilter(RpcRequest rpcRequest) {
        // 检查参数是否是null
        return Arrays.stream(rpcRequest.getParameters()).allMatch(Objects::nonNull);
    }
}
```



## Client注册中心缓存

服务列表会被Consumer客户端缓存，Mini-RPC使用发布订阅的方式保证缓存一致性。

## Zookeeper注册中心

Mini-RPC支持使用Zookeeper作为注册中心，具体的路径格式如下表所示：

| Path                                           | 作用                                     |
| ---------------------------------------------- | ---------------------------------------- |
| /mini-rpc/services/{{ServiceName}}/{{version}} | 服务根目录                               |
| 服务根目录/{{address}}                         | 服务Provider节点，data为Provider信息JSON |



Mini-RPC使用**CuratorFramework**的**TreeCacheListener**来监听Zookeeper注册中心节点的改变，以此来更新Consumer本地缓存。



## Redis注册中心

Mini-RPC支持使用Redis作为注册中心，Key-Value格式如下表所示：

| Key                                           | Value                                |
| --------------------------------------------- | ------------------------------------ |
| mini-rpc/services/{{serviceName}}/{{version}} | Hash，key是Provider地址，Value是JSON |

Mini-RPC使用Redis的**PSUBSCRIBE**来订阅服务列表的改变。

## SPI

Mini-RPC仿照Dubbo实现了SPI机制来加载扩展类。按照以下步骤即可实现SPI：

1. 编写SPI接口，并添加@SPI注解
2. 在META-INF/extensions目录下添加名称为扩展接口的文件
3. 编写SPI扩展类，并在扩展文件中添加名称与类名映射
4. 使用ExtensionLoader加载扩展类

```properties
extension1 = com.jay.test.extension.MyExtension1
```

```java
@SPI
public interface MyExtension {
    void hello();
}
```

```java
public class MyExtension1 implements MyExtension {
    @Override
    public void hello(){
        System.out.println("hello");
    }
}
```

```java
public class MyTest {
    
    @Test
    public void testExtension(){
        // 获取ExtensionLoader
        ExtensionLoader<MyExtension> extensionLoader = 	ExtensionLoader.getExtensionLoader(MyExtension.class);
        // 获取Extension
		MyExtension ext1 = extensionLoader.getExtension("ext1");
        
        ext1.hello();
    }
}
```

## 性能测试

