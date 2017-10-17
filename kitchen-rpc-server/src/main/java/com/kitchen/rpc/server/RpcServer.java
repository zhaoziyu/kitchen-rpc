package com.kitchen.rpc.server;

import com.kitchen.rpc.common.util.RpcStringUtil;
import com.kitchen.rpc.registry.store.RpcServiceRegistry;
import com.kitchen.rpc.registry.cache.RegistryCache;
import com.kitchen.rpc.registry.store.zookeeper.registry.ZooKeeperServiceRegistry;
import com.kitchen.rpc.server.config.RpcServerConfig;
import com.kitchen.rpc.common.exception.ProviderDeployException;
import com.kitchen.rpc.common.exception.RpcServiceException;
import com.kitchen.rpc.server.handler.ProtocolChannelInitializerHandler;
import com.kitchen.rpc.server.thread.BusinessThread;
import com.kitchen.rpc.server.util.LaunchArgName;
import com.kitchen.rpc.server.util.LaunchUtil;
import com.kitchen.rpc.server.util.RpcServerUtil;
import com.kitchen.rpc.server.deploy.ServerDeployFetcherFactory;
import com.kitchen.rpc.server.deploy.host.HostFetcher;
import com.kitchen.rpc.server.deploy.port.PortFetcher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

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
public class RpcServer implements ApplicationContextAware, InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServer.class);
    // Spring上下文
    public static ClassPathXmlApplicationContext APPLICATION_CONTEXT;

    // 是否开启RPC服务
    private static boolean OPEN_RPC = true;

    // RPC服务的名称（默认：provider）
    public static String RPC_NAME = "provider";

    // 服务端地址[ip:port]
    private InetSocketAddress localAddress;

    // 服务端权重
    private Integer serverWeight;

    /**
     * RPC服务注册的服务器（默认为ZooKeeper）
     * 通过Spring配置文件初始化
     */
    private static RpcServiceRegistry rpcServiceRegistry;

    // Netty通道启动后的返回结果
    private static ChannelFuture future = null;

    // Netty处理线程
    private static EventLoopGroup bossGroup = null;
    private static EventLoopGroup workerGroup = null;

    public RpcServer(boolean open, String name,
                     String serverHostType, String serverHost,
                     String serverPortType, Integer serverPort,
                     Integer serverWeight,
                     String registryCenterAddress) {
        OPEN_RPC = open;
        if (name != null && !name.isEmpty()) {
            RPC_NAME = name;
        }
        // 优先使用运行时传入参数
        if (LaunchUtil.existArg(LaunchArgName.ARG__REGISTRY)) {
            registryCenterAddress = LaunchUtil.getArgValue(LaunchArgName.ARG__REGISTRY);
        }
        if (LaunchUtil.existArg(LaunchArgName.ARG__SERVER_HOST_TYPE)) {
            serverHostType = LaunchUtil.getArgValue(LaunchArgName.ARG__SERVER_HOST_TYPE);
        }
        if (LaunchUtil.existArg(LaunchArgName.ARG__SERVER_HOST)) {
            serverHost = LaunchUtil.getArgValue(LaunchArgName.ARG__SERVER_HOST);
        }
        if (LaunchUtil.existArg(LaunchArgName.ARG__SERVER_PORT_TYPE)) {
            serverPortType = LaunchUtil.getArgValue(LaunchArgName.ARG__SERVER_PORT_TYPE);
        }
        if (LaunchUtil.existArg(LaunchArgName.ARG__SERVER_PORT)) {
            serverPort = Integer.valueOf(LaunchUtil.getArgValue(LaunchArgName.ARG__SERVER_PORT));
        }

        // 初始化RPC功能
        RegistryCache.setRpcServiceName(RPC_NAME);
        // 初始化宿主主机的Host和Port
        String ip = this.getHostIp(serverHostType, serverHost);
        if (ip == null) {
            // 服务退出
            shutdownService();
        }
        Integer port = this.getDeployPort(serverPortType, serverPort);
        if (port == null) {
            // 服务退出
            shutdownService();
        }
        localAddress = new InetSocketAddress(ip, port);
        String serverAddress = ip + ":" + port;
        RegistryCache.address(serverAddress);
        // 保存服务的权重
        this.serverWeight = serverWeight;
        RegistryCache.weight(this.serverWeight);

        if (OPEN_RPC) {
            // 初始化ZooKeeper目录服务
            // TODO 注册和发现通过“配置+抽象工厂”实现，支持多种注册中心
            rpcServiceRegistry = new ZooKeeperServiceRegistry(registryCenterAddress);
        } else {
            LOGGER.warn("未启用RPC服务，如需启用RPC服务，请在rpc-provider.properties中设置rpc.provider.open为true");
        }
    }

    private String getHostIp(String serverHostType, String serverHost) {
        HostFetcher hostFetcher = ServerDeployFetcherFactory.createHostFetcher(serverHostType);
        String ip = null;
        if (hostFetcher != null) {
            try {
                ip = hostFetcher.getIp(serverHost);
            } catch (ProviderDeployException e) {
                e.printStackTrace();
            }
        }
        return ip;
    }
    private Integer getDeployPort(String serverPortType, Integer serverPort) {
        PortFetcher portFetcher = ServerDeployFetcherFactory.createPortFetcher(serverPortType);
        Integer port = null;
        if (portFetcher != null) {
            try {
                port = portFetcher.getPort(serverPort);
            } catch (ProviderDeployException e) {
                e.printStackTrace();
            }
        }
        return port;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
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
        String log = "";
        RpcServerUtil.setContext(applicationContext);
        log += "完成服务扫描，共扫描到" + count + "个RpcService服务：";
        for (String key : serviceBeanMap.keySet()) {
            log += "\n" + key;
        }
        log += "\n——————————————————————";
        LOGGER.info(log);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
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
                LOGGER.error("初始化异常：服务地址绑定失败");
                shutdownService();
            }
        }

        if (OPEN_RPC) {
            // 注册RPC服务
            RegistryCache.registryCacheService(rpcServiceRegistry);
        }

        LOGGER.info("服务提供者(" + RPC_NAME + "  " + localAddress.getHostName() + ":" + localAddress.getPort() + ")：ServerBootstrap已启动完毕");

        // 等待...直到关闭
        future.channel().closeFuture().sync();
    }

    private static void shutdownService() {
        LOGGER.info("服务退出......");
        Runtime.getRuntime().exit(500);
    }

    /**
     * 优雅关闭服务提供者
     */
    public static void shutdownGracefully() {
        if (future != null) {
            future.channel().close();
        }
        if (rpcServiceRegistry != null) {
            rpcServiceRegistry.stop();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            LOGGER.info("Reactor线程池：完成优雅关闭（workerGroup）");
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            LOGGER.info("Reactor线程池：完成优雅关闭（bossGroup）");
        }

        // 关闭业务线程池
        BusinessThread.shutdown();
        LOGGER.info("RPC服务器：已关闭");

        // 关闭Spring上下文
        if (APPLICATION_CONTEXT != null) {
            APPLICATION_CONTEXT.destroy();
        }
    }
}
