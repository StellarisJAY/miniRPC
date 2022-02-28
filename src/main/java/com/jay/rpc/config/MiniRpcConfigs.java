package com.jay.rpc.config;

/**
 * <p>
 *  Configs
 * </p>
 *
 * @author Jay
 * @date 2022/02/08 12:52
 */
public class MiniRpcConfigs {

    private static final String REGISTRY_TYPE = "mini-rpc.registry.type";
    private static final String DEFAULT_REGISTRY_TYPE = "zookeeper";

    private static final String LOAD_BALANCE_TYPE = "mini-rpc.client.load-balance";
    private static final String DEFAULT_LOAD_BALANCE_TYPE = "consistent-hash";

    private static final String MAX_CONNECTIONS = "mini-rpc.client.max-conn";
    private static final int DEFAULT_MAX_CONNECTIONS = 2;

    private static final String ENABLE_SSL = "mini-rpc.enable-ssl";
    private static final boolean DEFAULT_ENABLE_SSL = false;

    private static final String SERVER_PORT = "mini-rpc.server.port";
    private static final int DEFAULT_SERVER_PORT = 9000;

    private static final String PROVIDER_GROUP = "mini-rpc.provider.group";

    private static final String PROVIDER_AS_REGISTRY = "mini-rpc.registry.provider-as-registry";
    private static final boolean DEFAULT_PROVIDER_AS_REGISTRY = false;

    private static final String SIMPLE_REGISTRY_HOST = "mini-rpc.registry.simple.host";
    private static final String SIMPLE_REGISTRY_PORT = "mini-rpc.registry.simple.port";
    private static final int DEFAULT_SIMPLE_REGISTRY_PORT = 9999;

    private static final String REDIS_HOST = "mini-rpc.registry.redis.host";
    private static final String REDIS_PORT = "mini-rpc.registry.redis.port";
    private static final int DEFAULT_REDIS_PORT = 6379;

    private static final String ZOOKEEPER_HOST = "mini-rpc.registry.zookeeper.host";
    private static final String ZOOKEEPER_PORT = "mini-rpc.registry.zookeeper.port";
    private static final int DEFAULT_ZOOKEEPER_PORT = 2187;

    public static final int ZOOKEEPER_SESSION_TIMEOUT = 3;
    public static final int ZOOKEEPER_CONNECTION_TIMEOUT = 3;

    private static final String PROMETHEUS_SERVER_PORT = "mini-rpc.prometheus.port";
    private static final int DEFAULT_PROMETHEUS_PORT = 9898;

    /**
     * 注册超时时间，60s
     */
    public static final long REGISTER_TIMEOUT = 60 * 1000;

    public static String registryType(){
        return ConfigsManager.get(REGISTRY_TYPE, DEFAULT_REGISTRY_TYPE);
    }

    public static String loadBalanceType(){
        return ConfigsManager.get(LOAD_BALANCE_TYPE, DEFAULT_LOAD_BALANCE_TYPE);
    }

    public static int maxConnections(){
        return ConfigsManager.getInt(MAX_CONNECTIONS, DEFAULT_MAX_CONNECTIONS);
    }

    public static boolean enableSsl(){
        return ConfigsManager.getBoolean(ENABLE_SSL, DEFAULT_ENABLE_SSL);
    }

    public static int serverPort(){
        return ConfigsManager.getInt(SERVER_PORT, DEFAULT_SERVER_PORT);
    }

    public static String providerGroup(){
        return ConfigsManager.get(PROVIDER_GROUP);
    }

    public static boolean providerAsRegistry(){
        return ConfigsManager.getBoolean(PROVIDER_AS_REGISTRY, DEFAULT_PROVIDER_AS_REGISTRY);
    }

    public static String simpleRegistryHost(){
        return ConfigsManager.get(SIMPLE_REGISTRY_HOST);
    }

    public static int simpleRegistryPort(){
        return ConfigsManager.getInt(SIMPLE_REGISTRY_PORT, DEFAULT_SIMPLE_REGISTRY_PORT);
    }

    public static String redisHost(){
        return ConfigsManager.get(REDIS_HOST);
    }

    public static int redisPort(){
        return ConfigsManager.getInt(REDIS_PORT, DEFAULT_REDIS_PORT);
    }

    public static String zookeeperHost(){
        return ConfigsManager.get(ZOOKEEPER_HOST);
    }

    public static int zookeeperPort(){
        return ConfigsManager.getInt(ZOOKEEPER_PORT, DEFAULT_ZOOKEEPER_PORT);
    }

    public static int prometheusServerPort(){
        return ConfigsManager.getInt(PROMETHEUS_SERVER_PORT, DEFAULT_PROMETHEUS_PORT);
    }
}
