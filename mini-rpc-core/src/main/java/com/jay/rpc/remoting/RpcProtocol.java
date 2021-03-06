package com.jay.rpc.remoting;

import com.jay.dove.transport.HeartBeatTrigger;
import com.jay.dove.transport.command.CommandCode;
import com.jay.dove.transport.command.CommandFactory;
import com.jay.dove.transport.command.CommandHandler;
import com.jay.dove.transport.protocol.*;

/**
 * <p>
 *  RPC协议
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 19:51
 */
public class RpcProtocol implements Protocol {

    /**
     * Mini-RPC 协议识别码
     */
    public static final ProtocolCode PROTOCOL_CODE = ProtocolCode.fromValue((byte)33);

    /**
     * 协议首部长度
     */
    public static final int HEADER_LENGTH = 24;

    /**
     * REQUEST 命令code
     */
    public static final CommandCode REQUEST = new CommandCode((short)1);
    /**
     * RESPONSE 命令code
     */
    public static final CommandCode RESPONSE = new CommandCode((short)2);
    /**
     * ERROR 命令code
     */
    public static final CommandCode ERROR = new CommandCode((short)3);
    /**
     * TIMEOUT 命令code
     */
    public static final CommandCode TIMEOUT = new CommandCode((short)4);

    public static final CommandCode LOOKUP = new CommandCode((short)5);

    public static final CommandCode REGISTER = new CommandCode((short)6);

    /**
     * 协议解码器
     */
    private final ProtocolDecoder decoder = new RpcDecoder();
    /**
     * 协议编码器
     */
    private final ProtocolEncoder encoder = new RpcEncoder();

    /**
     * 协议命令处理器
     */
    private final CommandHandler commandHandler;

    public RpcProtocol(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @Override
    public ProtocolEncoder getEncoder() {
        return encoder;
    }

    @Override
    public ProtocolM2mEncoder getM2mEncoder() {
        return null;
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
        return commandHandler;
    }

    @Override
    public HeartBeatTrigger getHeartBeatTrigger() {
        return null;
    }

    @Override
    public CommandFactory getCommandFactory() {
        return new RpcCommandFactory();
    }
}
