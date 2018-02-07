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
public class RpcServer implements ApplicationContextAware, InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private static Boolean rpcServerStarted = Boolean.FALSE;

    //确保不能被外部工程初始化
    protected RpcServer() {

    }

    @Autowired
    private RpcServerConfig config;

    /**
     * RPC服务注册的服务器（默认为ZooKeeper）
     * 通过Spring配置文件初始化
     */
    private RpcServiceRegistry rpcServiceRegistry;

    // Netty通道启动后的返回结果
    private static ChannelFuture future = null;

    // Netty处理线程
    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;


    private ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void scanRpcService() {
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
        RpcServerUtil.setContext(applicationContext);
        String log = "\n完成服务扫描";
        if (count > 0) {
            log += "，共扫描到" + count + "个Rpc服务";
        } else {
            log += "，未扫描到Rpc服务";
        }
        for (String key : serviceBeanMap.keySet()) {
            log += "\n" + key;
        }
        logger.info(log);
    }

    private void initRpcServer() {
        // 初始化RPC功能
        RegistryCache.setRpcServiceName(config.rpcName);

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
        String serverAddress = ip + ":" + port;
        RegistryCache.address(serverAddress);
        RegistryCache.weight(config.serverWeight);

        // 初始化服务注册中心
        // TODO 注册和发现通过“配置+抽象工厂”实现，支持多种注册中心
        rpcServiceRegistry = new ZooKeeperServiceRegistry(config.registry);


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
                logger.error("初始化异常：服务地址绑定失败");
                shutdownService();
            }
        }

        // 注册RPC服务
        RegistryCache.registryCacheService(rpcServiceRegistry);

        logger.info("服务提供者({}  {}:{})已启动完毕",
                config.rpcName,
                localAddress.getAddress().getHostAddress(),
                localAddress.getPort()
        );
    }

    /**
     * 主动关闭RpcServer，用于异常情况退出
     */
    private void shutdownService() {
        logger.info("服务退出......");
        Runtime.getRuntime().exit(500);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("RpcServer开始初始化……");

        if (!config.rpcOpen) {
            logger.warn("未启用RPC服务。如需启用，请在application.yml中设置相关配置");
            return;
        }
        scanRpcService();
        initRpcServer();
    }

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
        if (rpcServiceRegistry != null) {
            rpcServiceRegistry.stop();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            logger.info("Reactor线程池：完成优雅关闭（workerGroup）");
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            logger.info("Reactor线程池：完成优雅关闭（bossGroup）");
        }

        // 关闭业务线程池
        BusinessThread.shutdown();
        logger.info("RPC服务器：已关闭");
    }
}
