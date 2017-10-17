package com.kitchen.rpc.client.handler;

import com.kitchen.rpc.client.RpcCallback;
import com.kitchen.rpc.client.future.RpcClientFuture;
import com.kitchen.rpc.client.cache.ClientChannelCache;
import com.kitchen.rpc.client.thread.CallbackThreadPool;
import com.kitchen.rpc.common.RequestMode;
import com.kitchen.rpc.common.meta.RpcRequest;
import com.kitchen.rpc.common.meta.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC客户端请求发送处理器
 *
 * @date 2016-12-18
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class ClientChannelInboundHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientChannelInboundHandler.class);

    private ConcurrentHashMap<String, RpcClientFuture> pendingList = new ConcurrentHashMap<>();

    public RpcClientFuture sendRequest(Channel channel, RpcRequest request) {
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        String serviceAddress = remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();

        RpcClientFuture rpcFuture = new RpcClientFuture(request, serviceAddress);

        pendingList.put(request.getRequestId(), rpcFuture);
        channel.writeAndFlush(request);

        return rpcFuture;
    }

    public void sendRequest(Channel channel, RpcRequest request, List<RpcCallback> callbackList) {
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        String serviceAddress = remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();

        RpcClientFuture rpcFuture = new RpcClientFuture(request, serviceAddress);
        if (callbackList != null && callbackList.size() > 0) {
            rpcFuture.setCallbacks(callbackList);
        }
        pendingList.put(request.getRequestId(), rpcFuture);
        channel.writeAndFlush(request);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse) throws Exception {
        String requestId = rpcResponse.getRequestId();
        RpcClientFuture channelFuture = pendingList.get(requestId);
        if (channelFuture != null) {
            pendingList.remove(requestId);

            if (rpcResponse.getRequestMode() == RequestMode.SYNC) {
                // 同步请求需要释放锁
                channelFuture.done(rpcResponse);
            } else if (rpcResponse.getRequestMode() == RequestMode.ASYNC_CALLBACK) {
                // 异步回调请求需要处理回调函数
                handleCallback(
                        channelHandlerContext.channel(),
                        channelFuture.getCallbacks(),
                        rpcResponse
                );
            }
        }
    }

    private void handleCallback(Channel channel, List<RpcCallback> callbackList, RpcResponse response) {
        // 释放连接
        ClientChannelCache.getInstance().releaseChannel(channel);
        // 处理回调
        for (RpcCallback callback : callbackList) {
            CallbackThreadPool.handlerCallback(new Runnable() {
                @Override
                public void run() {
                    if (response.hasException()) {
                        callback.fail(response.getException());
                    } else {
                        callback.success(response.getResult());
                    }
                }
            });
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("ClientChannelInboundHandler捕获异常", cause);
        // 释放连接通道
        ClientChannelCache.getInstance().releaseChannel(ctx.channel());
        ctx.close();
    }
}
