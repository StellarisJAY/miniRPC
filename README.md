# Mini-RPC

Mini-RPC 是 基于Netty开发的 使用TCP通信的 RPC框架。提供了多种注册中心、多种调用方法、客户端负载均衡、服务端过滤器等功能。

- [x] 同步、异步、Future三种调用方法
- [x] SPI机制，提供插件化扩展功能
- [x] Zookeeper、Nacos、Redis三种注册中心支持
- [x] 注解化配置方式
- [x] 多种客户端负载均衡算法
- [x] 服务端过滤器，可注解化配置的过滤器链，提供exlusions和优先级配置

## 使用说明

### 概念说明

RPC系统中共有三种角色，注册中心、服务提供者（**Provider**）、服务消费者（**Consumer**）。

- **Provider**： 服务提供者，提供具体一种服务的服务器。
- **Service**：服务，在Mini-RPC中服务以 Java 类为载体，一个Java类就是一个服务。
- **Version**：服务版本，服务可以有不同的版本，因此同一个服务可以对应多个Java类。
- **Consumer**：服务消费者，通过RPC客户端调用远程服务。
- **Registry**：注册中心，消费者通过注册中心了解服务提供者的地址。Mini-RPC支持Zookeeper、Redis 和 Nacos作为注册中心。

### maven依赖

Mini-RPC提供SpringBoot支持，添加Mini-RPC-SpringBoot starter依赖即可使用。

```xml-dtd
<dependency>
	<groupId>com.jay</groupId>
	<artifactId>mini-rpc-spring-boot-starter</artifactId>
	<version>1.0-SNAPSHOT</version>
</dependency>
```

### 配置文件

在项目**Resources**目录下创建**mini-rpc.properties**文件

#### 通用配置

```properties
# 注册中心类型
mini-rpc.registry.type = redis/zookeeper/nacos
```

#### Provider配置

```properties
# provider 服务器端口
mini-rpc.server.port = 8888
```

#### Consumer配置

```properties
# 客户端负载均衡算法
mini-rpc.client.load-balance = random
# 客户端最大连接数，客户端会按照这个数量去建立连接
mini-rpc.client.max-conn = 10
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

MiniRPC支持同步、异步、Future三种调用方式。

#### 同步调用

使用MiniRpcProxy的createInstance方法创建RPC代理对象。该方法参数列表如下：

- 服务接口类
- 服务名称
- 版本号

```java
@Test
public void test(){
    // 调用 由 组hello-group中的服务器 提供的hello-service服务
	HelloService serviceV1 = (HelloService)MiniRpcProxy.createInstance(HelloService.class, "hello-service", 1);
	// 调用不同版本的服务
	HelloService serviceV2 = (HelloService)MiniRpcProxy.createInstance(HelloService.class, "hello-service", 2);

	log.info("v1: {}", serviceV1.sayHello("world"));
	log.info("v2: {}", serviceV2.sayHello("world"));
}
```

#### @RpcAutowired注解同步调用

使用RpcAutowired注解可以借助Spring容器来加载一个RPC代理对象，具体的用法如下：

```java
@RestController
public class TestController {
    // 在注解中指定调用服务的版本，也可以指定Provider地址
    @RpcAutowired(version = 1, provider="127.0.0.1:9999")
    private HelloService helloService;

    @GetMapping("/test/v1/{name}")
    public String testHelloV1(@PathVariable("name") String name){
        return helloService.hello(name);
    }
}
```

#### 异步调用

使用MiniRpcProxy的callAsync方法异步调用，该方法需要指定目标接口、版本号、方法、Callback和参数列表

```java
public static void callAsync(Class<?> targetClass, int version, Method method, AsyncCallback callback, Object[] args)
```

Callback需要实现AsyncCallback接口：

```java
public interface AsyncCallback {

    /**
     * 收到response
     * @param response {@link RpcResponse}
     */
    void onResponse(RpcResponse response);

    /**
     * 捕获到异常
     * @param throwable {@link Throwable}
     */
    void exceptionCaught(Throwable throwable);
}
```



#### Future调用

使用MiniRpcProxy的callFuture方法进行Future调用，该方法返回一个CompletableFuture对象。

```java
public static CompletableFuture<RpcResponse> callFuture(Class<?> targetClass, int version, Method method, Object[] args)
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



## Zookeeper注册中心

添加Zookeeper依赖，配置文件中修改配置

```xml-dtd
<dependency>
	<groupId>com.jay</groupId>
	<artifactId>mini-rpc-nacos-registry</artifactId>
	<version>1.0-SNAPSHOT</version>
</dependency>
```

```properties
# 注册中心类型
mini-rpc.registry.type = zookeeper
# zookeeper 
mini-rpc.registry.zookeeper.host = 127.0.0.1
mini-rpc.registry.zookeeper.port = 6379
```

服务注册后的Zookeeper节点如下表所示：

| Path                                           | 作用                                     |
| ---------------------------------------------- | ---------------------------------------- |
| /mini-rpc/services/{{ServiceName}}/{{version}} | 服务根目录                               |
| 服务根目录/{{address}}                         | 服务Provider节点，data为Provider信息JSON |

Mini-RPC使用**CuratorFramework**的**TreeCacheListener**来监听Zookeeper注册中心节点的改变，以此来更新Consumer本地缓存。



## Redis注册中心

添加Redis注册中心依赖，并在配置文件中配置Redis

```xml-dtd
<dependency>
	<groupId>com.jay</groupId>
	<artifactId>mini-rpc-redis-registry</artifactId>
	<version>1.0-SNAPSHOT</version>
</dependency>
```

```properties
# 注册中心类型
mini-rpc.registry.type = redis
# redis
mini-rpc.registry.redis.host = 127.0.0.1
mini-rpc.registry.redis.port = 6379
```

服务注册后，Redis中的Key Value如下表所示，

| Key                                           | Value                                |
| --------------------------------------------- | ------------------------------------ |
| mini-rpc/services/{{serviceName}}/{{version}} | Hash，key是Provider地址，Value是JSON |



## Nacos注册中心

添加Nacos注册中心依赖，并在配置文件中配置相关内容：

```xml-dtd
<dependency>
	<groupId>com.jay</groupId>
	<artifactId>mini-rpc-nacos-registry</artifactId>
	<version>1.0-SNAPSHOT</version>
</dependency>
```

```properties
# 注册中心类型
mini-rpc.registry.type = nacos
# Nacos
mini-rpc.registry.nacos.address = 127.0.0.1:8848
mini-rpc.registry.nacos.user = nacos
mini-rpc.registry.nacos.password = nacos
```



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

### 测试1

单线程发送10万次请求

```java
	@Test
    public void singleThread(){
        HelloService instance = (HelloService) MiniRpcProxy.createInstance(HelloService.class, 1);
		
        long testStart = System.currentTimeMillis();
        int loop = 100000;
        for(int i = 0; i < loop; i++){
            String hello = instance.hello("name");
            Assert.assertEquals("hello v1 name", hello);
        }
        long timeUsed = System.currentTimeMillis() - testStart;
        log.info("测试结束，用时：{}ms，QPS：{}", timeUsed, (loop * 1000) / timeUsed);
    }
```

测试结果如下，QPS为8000左右

```
2022-04-22 11:56:17,143 [main] INFO - 测试结束，用时：12125ms，QPS：8247
```

### 测试2

1000个线程并发，每个线程发送100次请求

```java
	@Test
    public void concurrent() throws InterruptedException {
        HelloService instance = (HelloService) MiniRpcProxy.createInstance(HelloService.class, 1);
        
        int threadCount = 1000;
        int loop = 100;
        int total = threadCount * loop;
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        Runnable task = ()->{
            for(int j = 0; j < loop; j++){
                String hello = instance.hello("name");
                Assert.assertEquals("hello v1 name", hello);
            }
            countDownLatch.countDown();
        };

        long testStart = System.currentTimeMillis();
        for(int i = 0; i < threadCount; i++){
            new Thread(task).start();
        }
        countDownLatch.await();
        long timeUsed = System.currentTimeMillis() - testStart;
        log.info("测试结束，用时：{}ms，QPS：{}", timeUsed, (total * 1000L) / timeUsed);
    }
```

测试结果如下，并发环境下的QPS大约为3万

```
2022-04-22 12:01:54,802 [main] INFO - 测试结束，用时：2772ms，QPS：36075
```

