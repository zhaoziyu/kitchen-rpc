package com.kitchen.rpc.registry.store.zookeeper.discovery;

import com.kitchen.rpc.registry.store.zookeeper.ZooKeeperConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZooKeeper连接状态监听器
 *
 * @author 赵梓彧 - kitchen_dev@163.com
 * @date 2017-03-15
 */
public class DiscoveryConnectionStateListener implements ConnectionStateListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryConnectionStateListener.class);
    public static boolean CONNECTED = false; //记录当前是否连接到Zookeeper的状态

    @Override
    public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            LOGGER.debug("已连接ZooKeeper");
            CONNECTED = true;

            ZooKeeperServiceDiscovery.watcherPath(ZooKeeperConfig.ZK_FIX_PATH_CHAIN__NODES);
        } else if (connectionState == ConnectionState.LOST) {
            try {
                LOGGER.debug("连接丢失，正在尝试重连");
                CONNECTED = false;
                // 关闭ZooKeeper节点检查者
                DiscoveryCurator.destroyProviderNodeCache();
                // 关闭ZooKeeper连接
                DiscoveryCurator.disconnectionZooKeeperServer();
                // 初始化ZooKeeper客户端
                ZooKeeperServiceDiscovery.initZooKeeperClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (connectionState == ConnectionState.RECONNECTED) { // Session 重连
            LOGGER.debug("已重新连接ZooKeeper");
            CONNECTED = true;
        }
    }
}
