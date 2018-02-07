package com.kitchen.rpc.client;


import com.kitchen.rpc.client.cache.ClientChannelCache;
import com.kitchen.rpc.client.config.RpcClientConfig;
import com.kitchen.rpc.client.thread.CallbackThreadPool;
import com.kitchen.rpc.registry.policy.PolicyConfig;
import com.kitchen.rpc.registry.store.RpcServiceDiscovery;
import com.kitchen.rpc.registry.store.zookeeper.discovery.ZooKeeperServiceDiscovery;
import io.netty.util.ThreadDeathWatcher;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Lazy(false)
class RpcClient implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(RpcClientProxy.class);

    @Autowired
    private RpcClientConfig config;

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("RpcClient开始初始化……");
        if (config.rpcOpen) {
            // TODO 是否可以通过Spring注入至PolicyConfig，或在PolicyConfig中使用@Value注解
            PolicyConfig.SERVICE_LB_POLICY = config.SERVICE_LB_POLICY;
            // TODO 注册和发现通过“配置+抽象工厂”实现，支持多种注册中心
            RpcServiceDiscovery rpcServiceDiscovery = new ZooKeeperServiceDiscovery(config.registryCenterAddress, config.rpcName);
            ClientChannelCache.setRpcServiceDiscovery(rpcServiceDiscovery);
        } else {
            logger.info("未启用RPC服务，如需启用RPC服务，请在application.yml中设置相关配置");
        }
    }


    @Override
    public void destroy() {
        if (config.rpcOpen) {
            logger.info("回收RPC相关资源");
            if (ClientChannelCache.getRpcServiceDiscovery() != null) {
                ClientChannelCache.getRpcServiceDiscovery().stop();
            }
            if (ClientChannelCache.isRuning()) {
                ClientChannelCache.getInstance().stop();
            }
        }
        CallbackThreadPool.stop();

        try {
            GlobalEventExecutor.INSTANCE.awaitInactivity(5L, TimeUnit.SECONDS);
            ThreadDeathWatcher.awaitInactivity(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn(e.getMessage());
        }
    }
}
