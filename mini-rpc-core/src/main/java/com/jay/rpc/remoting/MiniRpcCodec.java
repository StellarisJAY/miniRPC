package com.jay.rpc.remoting;

import com.jay.dove.transport.codec.Codec;
import com.jay.dove.transport.codec.ProtocolCodeBasedDecoder;
import com.jay.dove.transport.codec.ProtocolCodeBasedEncoder;
import io.netty.channel.ChannelHandler;

/**
 * <p>
 *  Mini RPC Codec
 * </p>
 *
 * @author Jay
 * @date 2022/02/06 19:53
 */
public class MiniRpcCodec implements Codec {
    @Override
    public ChannelHandler newDecoder() {
        return new ProtocolCodeBasedDecoder();
    }

    @Override
    public ChannelHandler newEncoder() {
        return new ProtocolCodeBasedEncoder(RpcProtocol.PROTOCOL_CODE);
    }
}
