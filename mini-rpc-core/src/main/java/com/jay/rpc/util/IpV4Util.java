package com.jay.rpc.util;

import com.jay.rpc.config.MiniRpcConfigs;

import java.net.Inet4Address;
import java.net.UnknownHostException;

/**
 * <p>
 *
 * </p>
 *
 * @author Jay
 * @date 2022/05/10 16:25
 */
public class IpV4Util {
    public static String getIpV4Address(){
        try{
            return Inet4Address.getLocalHost().getHostAddress() + ":" + MiniRpcConfigs.serverPort();
        }catch (UnknownHostException e){
            return "127.0.0.1:" + MiniRpcConfigs.serverPort();
        }

    }
}
