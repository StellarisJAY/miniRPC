package com.jay.rpc.remoting;

import com.jay.dove.transport.command.CommandCode;
import com.jay.dove.transport.command.RemotingCommand;
import lombok.Builder;
import lombok.ToString;

/**
 * <p>
 *  Mini RPC Command
 *  +---------+----------+--------+------+-----------+-------------+------------+---------+
 *  |  proto  |  length  |  code  |  id  |  timeout  |  serialize  |  compress  |  crc32  |
 *  +---------+----------+--------+------+-----------+-------------+------------+---------+
 *  |                            content                                                  |
 *  +-------------------------------------------------------------------------------------+
 *  proto：协议码，1 byte
 *  length：报文长度，4 byte
 *  code：报文类型code，2 byte
 *  id：报文id，4 byte
 *  timeout：超时时间戳，8 byte
 *  serialize：序列化器编号，1 byte
 *  compress：压缩器编号，1 byte
 *  crc32：crc32校验码，4 byte
 *
 *  首部长度：25 bytes
 *
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 19:52
 */
@Builder
@ToString
public class RpcRemotingCommand implements RemotingCommand {

    private int length;
    private CommandCode commandCode;
    private int id;
    private long timeout;
    private byte serializer;
    private byte compressor;
    private int crc32;

    private byte[] content;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public byte getSerializer() {
        return serializer;
    }

    @Override
    public CommandCode getCommandCode() {
        return commandCode;
    }

    @Override
    public long getTimeoutMillis() {
        return timeout;
    }

    @Override
    public void setTimeoutMillis(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    public int getLength() {
        return length;
    }

    public byte getCompressor() {
        return compressor;
    }

    public int getCrc32() {
        return crc32;
    }
}
