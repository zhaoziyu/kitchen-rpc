package com.kitchen.rpc.common.codec.protostuff;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * RPC 编码器
 *
 * @date 2016-12-02
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class ProtocolEncoder extends MessageToByteEncoder<Object> {
    private Class<?> genericClass;

    public ProtocolEncoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)) {
            byte[] data = ProtocolUtil.serialize(in);
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }
}
