package com.jay.rpc.remoting;

import com.jay.dove.transport.connection.AbstractConnectionFactory;
import com.jay.dove.transport.connection.ConnectEventHandler;

/**
 * <p>
 *  RPC客户端连接工厂
 * </p>
 *
 * @author Jay
 * @date 2022/02/07 11:47
 */
public class RpcConnectionFactory extends AbstractConnectionFactory {
    public RpcConnectionFactory() {
        super(new MiniRpcCodec(), RpcProtocol.PROTOCOL_CODE, new ConnectEventHandler());
    }
}
