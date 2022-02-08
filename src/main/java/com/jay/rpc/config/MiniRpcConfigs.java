package com.jay.rpc.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * <p>
 *  Configs
 * </p>
 *
 * @author Jay
 * @date 2022/02/08 12:52
 */
public class MiniRpcConfigs {

    private static Properties properties;
    /**
     * 注册超时时间，60s
     */
    public static final long REGISTER_TIMEOUT = 60 * 1000;
    static{
        ClassLoader classLoader = MiniRpcConfigs.class.getClassLoader();
        try(InputStream stream = classLoader.getResourceAsStream("mini-rpc.properties")){
            properties = new Properties();
            properties.load(stream);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static String get(String name){
        return properties.getProperty(name);
    }

    public static int getInt(String name){
        return Integer.parseInt(get(name));
    }
}
