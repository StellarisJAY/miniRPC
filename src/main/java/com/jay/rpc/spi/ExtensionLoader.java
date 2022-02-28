package com.jay.rpc.spi;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 *  SPI Extension Loader
 * </p>
 *
 * @author Jay
 * @date 2022/02/28 10:22
 */
@Slf4j
public class ExtensionLoader<T> {
    /**
     * extension文件目录
     */
    public static final String SERVICE_DIRECTORY = "META-INF/extensions/";
    /**
     * ExtensionLoader缓存
     */
    private static final ConcurrentHashMap<Class<?>, ExtensionLoader<?>> LOADER_CACHE = new ConcurrentHashMap<>();
    /**
     * Extension 实例缓存
     */
    private static final ConcurrentHashMap<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, InstanceHolder<T>> instanceCache = new ConcurrentHashMap<>();
    /**
     * Extension类缓存
     */
    private volatile Map<String, Class<?>> extensionClasses = null;
    private final Object mutex = new Object();
    private final Class<T> type;

    /**
     * 获取一个类型的ExtensionLoader
     * @param clazz 类型，必须@SPI
     * @param <S> type
     * @return {@link ExtensionLoader}
     */
    @SuppressWarnings("unchecked")
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> clazz){
        if(clazz == null){
            throw new NullPointerException("extension class can not be null");
        }
        if(!clazz.isInterface()){
            throw new IllegalArgumentException("extension type must be interface");
        }
        if(!clazz.isAnnotationPresent(SPI.class)){
            throw new IllegalArgumentException("extension type must be annotated by @SPI");
        }
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) LOADER_CACHE.get(clazz);
        if(extensionLoader == null){
            LOADER_CACHE.putIfAbsent(clazz, new ExtensionLoader<>(clazz));
            extensionLoader = (ExtensionLoader<S>) LOADER_CACHE.get(clazz);
        }
        return extensionLoader;
    }

    private ExtensionLoader(Class<T> type){
        this.type = type;
    }

    /**
     * 获取extension
     * @param name 名称
     * @return T
     */
    public T getExtension(String name){
        InstanceHolder<T> holder = instanceCache.get(name);
        if(holder == null){
            instanceCache.putIfAbsent(name, new InstanceHolder<>());
            holder = instanceCache.get(name);
        }
        T instance = holder.get();
        if(instance == null){
            synchronized (holder){
                instance = holder.get();
                if(instance == null){
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private T createExtension(String name){
        Class<?> extensionClass = getExtensionClasses().get(name);
        if(extensionClass == null){
            throw new RuntimeException("can't find extension class for: " + name);
        }
        Object instance = INSTANCES.get(extensionClass);

        if(instance == null){
            try{
                INSTANCES.putIfAbsent(extensionClass, extensionClass.newInstance());
                instance = INSTANCES.get(extensionClass);
            }catch (Exception e){
                log.error("create extension instance error ", e);
            }
        }
        return (T)instance;
    }

    private Map<String, Class<?>> getExtensionClasses(){
        if(extensionClasses == null){
            synchronized (mutex){
                if(extensionClasses == null){
                    extensionClasses = loadDirectory();
                }
            }
        }
        return extensionClasses;
    }

    private Map<String, Class<?>> loadDirectory(){
        HashMap<String, Class<?>> classes = new HashMap<>(16);
        String fileName = SERVICE_DIRECTORY + type.getName();
        ClassLoader classLoader = this.getClass().getClassLoader();
        try{
            URL url = classLoader.getResource(fileName);
            loadResource(classes, classLoader, url);
        }catch (Exception e){
            log.error("load extension directory error ", e);
        }
        return classes;
    }

    private void loadResource(Map<String, Class<?>> classes, ClassLoader classLoader, URL url){
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))){
            String line;
            while((line = reader.readLine()) != null){
                // 跳过注释行
                int idx = line.indexOf("#");
                if(idx == -1){
                    // 拆分kv对
                    String[] parts = line.split("=");
                    if(parts.length == 2){
                        String name = parts[0];
                        String className = parts[1];
                        if(name.length() > 0 && className.length() > 0){
                            classes.put(name, classLoader.loadClass(className));
                        }
                    }
                }
            }
        }catch (IOException e){
            log.error("load extension file error ", e);
        } catch (ClassNotFoundException e) {
            log.error("load extension class error ", e);
        }
    }
}
