package com.kitchen.rpc.client.handler;

import com.kitchen.rpc.common.codec.protostuff.ProtocolDecoder;
import com.kitchen.rpc.common.codec.protostuff.ProtocolEncoder;
import com.kitchen.rpc.common.meta.RpcRequest;
import com.kitchen.rpc.common.meta.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC连接通道池的处理器
 *
 * @date 2016-12-18
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class ProtocolChannelPoolHandler implements ChannelPoolHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolChannelPoolHandler.class);

    @Override
    public void channelReleased(Channel channel) throws Exception {
        LOGGER.debug("释放连接到连接池：" + channel.id());
    }

    @Override
    public void channelAcquired(Channel channel) throws Exception {
        LOGGER.debug("从连接池获取连接：" + channel.id());
    }

    @Override
    public void channelCreated(Channel channel) throws Exception {
        // 解码 RPC 响应
        channel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
        channel.pipeline().addLast(new ProtocolDecoder(RpcResponse.class));
        // 编码 RPC 请求
        channel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
        channel.pipeline().addLast(new ProtocolEncoder(RpcRequest.class));
        // 处理 RPC 响应
        channel.pipeline().addLast(new ClientChannelInboundHandler());

        LOGGER.debug("创建新的连接通道：" + channel.id());
    }
}
