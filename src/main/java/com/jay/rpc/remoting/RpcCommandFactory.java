package com.jay.rpc.remoting;

import com.jay.dove.transport.command.CommandCode;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.dove.transport.command.RemotingCommand;

/**
 * <p>
 *
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 19:52
 */
public class RpcCommandFactory implements CommandFactory {
    @Override
    public RemotingCommand createRequest(Object data, CommandCode commandCode) {
        return null;
    }

    @Override
    public RemotingCommand createResponse(int i, Object o, CommandCode commandCode) {
        return null;
    }

    @Override
    public RemotingCommand createTimeoutResponse(int i, Object o) {
        return null;
    }

    @Override
    public RemotingCommand createExceptionResponse(int i, String s) {
        return null;
    }

    @Override
    public RemotingCommand createExceptionResponse(int i, Throwable throwable) {
        return null;
    }
}
