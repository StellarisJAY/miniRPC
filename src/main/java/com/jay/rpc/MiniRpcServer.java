package com.jay.rpc;

import com.jay.dove.DoveServer;
import com.jay.dove.common.AbstractLifeCycle;
import com.jay.dove.transport.codec.Codec;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.rpc.remoting.MiniRpcCodec;
import com.jay.rpc.remoting.RpcCommandFactory;

/**
 * <p>
 *  Mini-RPC 服务端
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 20:24
 */
public class MiniRpcServer extends AbstractLifeCycle {
    /**
     * dove 服务器
     */
    private final DoveServer server;

    public MiniRpcServer(int port) {
        CommandFactory commandFactory = new RpcCommandFactory();
        Codec miniRpcCodec = new MiniRpcCodec();
        this.server = new DoveServer(miniRpcCodec, port, commandFactory);
    }

    @Override
    public void startup() {
        super.startup();
        this.server.startup();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        this.server.shutdown();
    }
}
