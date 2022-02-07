package com.jay.rpc.service;

import com.jay.rpc.annotation.RpcService;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * <p>
 *  默认服务扫描器
 *  不依赖Spring容器，会单独扫描整个项目
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 21:01
 */
public class SimpleServiceScanner implements ServiceScanner{

    private ServiceMapping serviceMapping;

    @Override
    public void doScan(String path) throws Exception {
        ClassLoader classLoader = SimpleServiceScanner.class.getClassLoader();
        URL resource = classLoader.getResource(path);
        if(resource != null){
            if("jar".equalsIgnoreCase(resource.getProtocol())){
                // 打开jar包
                JarURLConnection urlConnection = (JarURLConnection)resource.openConnection();
                JarFile jarFile = urlConnection.getJarFile();
                Enumeration<JarEntry> entries = jarFile.entries();
                // 遍历jar包资源
                while(entries.hasMoreElements()){
                    JarEntry entry = entries.nextElement();
                    // 处理class文件
                    if(entry.getName().startsWith(path) && entry.getName().endsWith(".class")){
                        String entryName = entry.getName();
                        String className = entryName.substring(0, entryName.indexOf(".class")).replace("/", ".");
                        // 获取class对象
                        Class<?> clazz = Class.forName(className);
                        if(!clazz.isAnnotation() && !clazz.isInterface() && clazz.isAnnotationPresent(RpcService.class)){
                            RpcService annotation = clazz.getAnnotation(RpcService.class);
                            String serviceName = annotation.name();
                            int version = annotation.version();
                            ServiceInfo serviceInfo = new ServiceInfo(serviceName, version);
                            // 创建对象实例
                            Object instance = clazz.newInstance();
                            ServiceInstance serviceInstance = new ServiceInstance(clazz, instance);
                            getServiceMapping().registerServiceInstance(serviceInfo, serviceInstance);
                        }
                    }
                }
            }else{
                File file = new File(resource.getFile());
                File[] files;
                if(file.isDirectory() && (files = file.listFiles()) != null){
                    String rootPath = path + "/";
                    for(File ls : files){
                        doScan(rootPath + ls.getName());
                    }
                }
                else if(file.getName().endsWith(".class")){
                    String fullPath = path.replace("/", ".") + "." + file.getName();
                    String className = fullPath.substring(0, fullPath.indexOf(".class"));
                    Class<?> clazz = classLoader.loadClass(className);
                    if(!clazz.isAnnotation() && !clazz.isInterface() && clazz.isAnnotationPresent(RpcService.class)){
                        RpcService annotation = clazz.getAnnotation(RpcService.class);
                        String serviceName = annotation.name();
                        int version = annotation.version();

                        Object instance = clazz.newInstance();
                        ServiceInfo serviceInfo = new ServiceInfo(serviceName, version);
                        ServiceInstance serviceInstance = new ServiceInstance(clazz, instance);
                        getServiceMapping().registerServiceInstance(serviceInfo, serviceInstance);
                    }
                }
            }
        }else{
            throw new Exception("can't find resource from this package");
        }
    }

    @Override
    public ServiceMapping getServiceMapping() {
        return serviceMapping;
    }

    @Override
    public void setServiceMapping(ServiceMapping serviceMapping) {
        this.serviceMapping = serviceMapping;
    }
}
