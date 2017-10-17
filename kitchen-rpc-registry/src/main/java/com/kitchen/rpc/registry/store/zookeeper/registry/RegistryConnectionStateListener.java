package com.kitchen.rpc.registry.store.zookeeper.registry;

import com.kitchen.rpc.registry.store.RpcServiceRegistry;
import com.kitchen.rpc.registry.cache.RegistryCache;
import com.kitchen.rpc.registry.store.zookeeper.ZooKeeperConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自定义连接状态监听器
 * 当发生Session重连时，重连成功后，重新将服务暴露给ZooKeeper服务器
 * 避免Session重连后，ZooKeeper服务器将临时节点删除，客户端找不到服务提供者的问题
 *
 * @date 2016-12-03
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class RegistryConnectionStateListener implements ConnectionStateListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryConnectionStateListener.class);

    private RpcServiceRegistry rpcServiceRegistry;

    public RegistryConnectionStateListener(RpcServiceRegistry rpcServiceRegistry) {
        this.rpcServiceRegistry = rpcServiceRegistry;
    }

    @Override
    public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            LOGGER.debug("ZooKeeper Registry State:CONNECTED");
            try {
                String registryPath = ZooKeeperConfig.ZK_FIX_PATH__PROVIDER_POOL;
                if (!RegistryCurator.checkNodeExist(registryPath)) {
                    // registry 节点不存在，则创建（持久）
                    RegistryCurator.createFixNode(registryPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (connectionState == ConnectionState.RECONNECTED) { // Session 重连
            LOGGER.debug("ZooKeeper Registry State:RECONNECTED");
            while (true) {
                try {
                    if (curatorFramework.getZookeeperClient().blockUntilConnectedOrTimedOut()) {
                        // 已重连成功，注册 RPC 服务地址到ZooKeeper服务器
                        RegistryCache.registryCacheService(rpcServiceRegistry);
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
