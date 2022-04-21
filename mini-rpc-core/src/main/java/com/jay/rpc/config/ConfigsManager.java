package com.jay.rpc.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * <p>
 *  配置管理器
 * </p>
 *
 * @author Jay
 * @date 2022/02/28 9:46
 */
public class ConfigsManager {
    private static Properties properties;

    static{
        ClassLoader classLoader = MiniRpcConfigs.class.getClassLoader();
        try(InputStream stream = classLoader.getResourceAsStream("mini-rpc.properties")){
            properties = new Properties();
            properties.load(stream);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static String get(String name, String defaultValue){
        String value = properties.getProperty(name);
        return value != null ? value : defaultValue;
    }

    public static String get(String name){
        return properties.getProperty(name);
    }

    public static int getInt(String name, int defaultValue){
        String value = get(name);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    public static boolean getBoolean(String name, boolean defaultValue){
        String property = get(name);
        return property != null ? Boolean.parseBoolean(property) : defaultValue;
    }

    public static Properties getProperties(){
        return properties;
    }
}
