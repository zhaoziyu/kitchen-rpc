package com.kitchen.rpc.client.cache;

import com.kitchen.rpc.client.config.RpcClientConfig;
import com.kitchen.rpc.common.util.RpcStringUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty客户端工厂类
 *
 * @date 2016-12-18
 * @author 赵梓彧 - kitchen_dev@163.com
 */
class ClientBootstrapFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientBootstrapFactory.class);
    /**
     * 客户端IO处理线程池
     * 默认线程数量：当前服务器的处理器核数 * 2
     *
     */
    private static EventLoopGroup eventLoopGroup = new NioEventLoopGroup(RpcClientConfig.RPC_CLIENT_IO_THREADS);

    public static Bootstrap createNewBootstrap(String serviceAddress) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);

        // 连接设置
        bootstrap.option(ChannelOption.TCP_NODELAY, true);// 通过NoDelay禁用Nagle,使消息立即发出去，不用等待到一定的数据量才发出去
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);// 长连接

        String host = RpcStringUtil.getHost(serviceAddress);
        int port = RpcStringUtil.getPort(serviceAddress);
        bootstrap.remoteAddress(host, port);

        return bootstrap;
    }

    public static void stopEventLoopGroup() {
        if (eventLoopGroup != null) {
            try {
                eventLoopGroup.shutdownGracefully().sync();
                Thread.sleep(1000);// 等待GlobalEventExecutor关闭
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOGGER.info("NioEventLoopGroup：优雅关闭");
        }
    }
}
