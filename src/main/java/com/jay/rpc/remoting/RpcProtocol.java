package com.jay.rpc.remoting;

import com.jay.dove.transport.HeartBeatTrigger;
import com.jay.dove.transport.command.CommandCode;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.dove.transport.command.CommandHandler;
import com.jay.dove.transport.protocol.Protocol;
import com.jay.dove.transport.protocol.ProtocolCode;
import com.jay.dove.transport.protocol.ProtocolDecoder;
import com.jay.dove.transport.protocol.ProtocolEncoder;

/**
 * <p>
 *
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 19:51
 */
public class RpcProtocol implements Protocol {

    /**
     * Mini-RPC protocol code
     */
    public static final ProtocolCode PROTOCOL_CODE = ProtocolCode.fromValue((byte)33);
    public static final int HEADER_LENGTH = 25;

    public static final CommandCode REQUEST = new CommandCode((short)1);
    public static final CommandCode RESPONSE = new CommandCode((short)2);

    private final ProtocolDecoder decoder = new RpcDecoder();
    private final ProtocolEncoder encoder = new RpcEncoder();

    @Override
    public ProtocolEncoder getEncoder() {
        return encoder;
    }

    @Override
    public ProtocolDecoder getDecoder() {
        return decoder;
    }

    @Override
    public ProtocolCode getCode() {
        return PROTOCOL_CODE;
    }

    @Override
    public CommandHandler getCommandHandler() {
        return null;
    }

    @Override
    public HeartBeatTrigger getHeartBeatTrigger() {
        return null;
    }
}
