package com.kitchen.rpc.server.handler;

import com.kitchen.rpc.common.codec.protostuff.ProtocolDecoder;
import com.kitchen.rpc.common.codec.protostuff.ProtocolEncoder;
import com.kitchen.rpc.common.meta.RpcRequest;
import com.kitchen.rpc.common.meta.RpcResponse;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * @author 赵梓彧 - kitchen_dev@163.com
 * @date 2017-06-24
 */
public class ProtocolChannelInitializerHandler extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel channel) throws Exception {
        // 解码 RPC 请求
        channel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
        channel.pipeline().addLast(new ProtocolDecoder(RpcRequest.class));
        // 编码 RPC 响应
        channel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
        channel.pipeline().addLast(new ProtocolEncoder(RpcResponse.class));
        // 处理 RPC 请求
        channel.pipeline().addLast(new ServerChannelInboundHandler());
    }
}
