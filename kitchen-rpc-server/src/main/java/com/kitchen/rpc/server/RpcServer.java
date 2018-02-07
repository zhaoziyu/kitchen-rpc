package com.kitchen.rpc.server;

import com.kitchen.rpc.common.util.RpcStringUtil;
import com.kitchen.rpc.registry.store.RpcServiceRegistry;
import com.kitchen.rpc.registry.cache.RegistryCache;
import com.kitchen.rpc.registry.store.zookeeper.registry.ZooKeeperServiceRegistry;
import com.kitchen.rpc.server.config.RpcServerConfig;
import com.kitchen.rpc.common.exception.RpcServiceException;
import com.kitchen.rpc.server.handler.ProtocolChannelInitializerHandler;
import com.kitchen.rpc.server.thread.BusinessThread;
import com.kitchen.rpc.server.util.RpcServerUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * RPC 服务器（用于发布 RPC 服务）
 * 单例模式
 *
 * 执行顺序：
 * 1、RpcServer()
 * 2、setApplicationContext()
 * 3、afterPropertiesSet()
 *
 * @date 2016-12-02
 * @author 赵梓彧 - kitchen_dev@163.com
 */
@Component
@Lazy(false)
@Scope("singleton")
public class RpcServer implements ApplicationContextAware, InitializingBean, DisposableBean {
    //确保不能被外部工程初始化
    protected RpcServer() {

    }
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private static Boolean rpcServerStarted = Boolean.FALSE;

    @Autowired
    private RpcServerConfig config;

    // Netty通道启动后的返回结果
    private static ChannelFuture future = null;

    // Netty处理线程
    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;


    private ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        scanRpcService();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("<RpcServer>: 开始初始化……");

        if (!config.rpcOpen) {
            logger.warn("<RpcServer>: 未启用RPC服务,如需启用,请在application.yml中设置相关配置");
            return;
        }

        InetSocketAddress address = getHostSocketAddress();

        initRpcServer(address);

        initServiceRegistry(address);
    }

    private void scanRpcService() {
        RpcServerUtil.setContext(applicationContext);

        // 扫描带有 RpcService 注解的类并初始化 RegistryCache 对象
        int count = 0;
        Map<String, Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(RpcService.class);
        if (MapUtils.isNotEmpty(serviceBeanMap)) {
            for (Object serviceBean : serviceBeanMap.values()) {
                // 获取RpcService注解类的实现接口
                Class<?>[] interfaces = serviceBean.getClass().getInterfaces();
                if (interfaces.length != 1) {
                    throw new RpcServiceException("RPC服务接口实现错误:" + serviceBean.getClass().getName());
                }

                RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);

                String serviceName = interfaces[0].getName();
                String serviceVersion = rpcService.version();
                if (RpcStringUtil.isNotEmpty(serviceVersion)) {
                    serviceName += "-" + serviceVersion;
                }

                // 验证是否存在相同服务接口相同版本号的服务实现
                if (RegistryCache.get().containsKey(serviceName)) {
                    throw new RpcServiceException("重复的RPC服务接口实现:" + serviceName);
                }

                RegistryCache.put(serviceName, serviceBean);
                count++;
            }
        }
        String log = "<RpcServer>: 完成Rpc服务扫描";
        if (count > 0) {
            log += "，共扫描到" + count + "个服务";
        } else {
            log += "，未扫描到服务";
        }
        for (String key : serviceBeanMap.keySet()) {
            log += "\n" + key;
        }
        logger.info(log);
    }

    private InetSocketAddress getHostSocketAddress() {
        // 初始化宿主主机的Host和Port
        String ip = AddressFetcherUtil.getHostIp(config.serverHostType, config.serverHostIp);
        if (ip == null) {
            // 服务退出
            shutdownService();
        }
        Integer port = AddressFetcherUtil.getDeployPort(config.serverPortType, config.serverPortNumber);
        if (port == null) {
            // 服务退出
            shutdownService();
        }
        // 服务端地址
        InetSocketAddress localAddress = new InetSocketAddress(ip, port);

        return localAddress;
    }
    private void initServiceRegistry(InetSocketAddress address) {
        // 初始化RPC功能
        RegistryCache.setRpcServiceName(config.rpcName);

        String serverAddress = address.getAddress().getHostAddress() + ":" + address.getPort();
        RegistryCache.address(serverAddress);
        RegistryCache.weight(config.serverWeight);

        // 初始化服务注册中心
        // TODO 注册和发现通过“配置+抽象工厂”实现，支持多种注册中心
        RpcServiceRegistry rpcServiceRegistry = new ZooKeeperServiceRegistry(config.registry);

        // 注册RPC服务
        RegistryCache.registryCacheService(rpcServiceRegistry);
    }
    private void initRpcServer(InetSocketAddress localAddress) throws InterruptedException {
        // 创建并初始化 Netty 服务端 Bootstrap 对象
        ServerBootstrap bootstrap = new ServerBootstrap();
        // 设置并绑定Reactor线程池（注：也可以只创建一个共享线程池），Netty默认线程数是“Java虚拟机可用的处理器数量的2倍”
        bossGroup = new NioEventLoopGroup();// 用来接收客户端的连接（bossGroup中有多个NioEventLoop线程，每个NioEventLoop绑定一个端口，也就是说，如果程序只需要监听1个端口的话，bossGroup里面只需要有一个NioEventLoop线程就行了。）
        workerGroup = new NioEventLoopGroup(RpcServerConfig.RPC_SERVER_WORK_THREADS);// 用来处理已经接收的客户端连接
        bootstrap.group(bossGroup, workerGroup);

        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new ProtocolChannelInitializerHandler());
        bootstrap.option(ChannelOption.SO_BACKLOG, RpcServerConfig.CHANNEL_BACKLOG);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);//保持长连接状态

        // 设置RPC服务地址[ip:port]
        bootstrap.localAddress(localAddress);

        // 启动 RPC 服务器
        try {
            future = bootstrap.bind().sync();
        } catch (Exception e) {
            if (e instanceof BindException) {
                logger.error("<RpcServer>: 初始化异常：服务地址绑定失败");
                shutdownService();
            }
        }

        logger.info("<RpcServer>: 服务提供者({}  {}:{})已启动完毕,开始监听请求",
                config.rpcName,
                localAddress.getAddress().getHostAddress(),
                localAddress.getPort()
        );
    }

    /**
     * 主动关闭RpcServer，用于异常情况退出
     */
    private void shutdownService() {
        logger.info("<RpcServer>: 服务退出......");
        Runtime.getRuntime().exit(500);
    }

    /**
     * TODO 需验证netty是否需要阻塞主线程
     * 原来在初始化最后添加"future.channel().closeFuture().sync();"，是为了将主线程阻塞，避免程序退出
     * 20180207：在做springboot适配时，发现，即使不调用上述语句，主线程依然不会退出，验证未发现不添加此语句会有什么影响
     */
    @Deprecated
    public static void start() {
        if (future == null) {
            logger.warn("未启用RPC服务。如需启用，请在application.yml中设置相关配置");
            return;
        }
        if (rpcServerStarted) {
            logger.warn("RPC服务仅需启动一次");
            return;
        }
        // 等待...直到关闭
        try {
            rpcServerStarted = Boolean.TRUE;
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 优雅关闭服务提供者
     */
    @Override
    public void destroy() throws Exception {
        if (future != null) {
            future.channel().close();
        }
        RegistryCache.destroy();

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            logger.info("<RpcServer>: Netty线程池,完成优雅关闭（workerGroup）");
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            logger.info("<RpcServer>: Netty线程池,完成优雅关闭（bossGroup）");
        }

        // 关闭业务线程池
        BusinessThread.shutdown();
        logger.info("<RpcServer>: RPC服务器已关闭");
    }
}
