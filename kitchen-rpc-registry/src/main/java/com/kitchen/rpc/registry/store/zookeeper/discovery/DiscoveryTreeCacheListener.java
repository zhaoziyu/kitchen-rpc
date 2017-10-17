package com.kitchen.rpc.registry.store.zookeeper.discovery;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * 监听ZooKeeper的节点状态
 *
 * @author 赵梓彧 - kitchen_dev@163.com
 * @date 2017-03-15
 */
public class DiscoveryTreeCacheListener implements TreeCacheListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryTreeCacheListener.class);

    private final static Charset CHARSET = Charset.forName("UTF-8");
    private static boolean initialized = false;

    @Override
    public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {
        if (treeCacheEvent == null) {
            return;
        }

        boolean updated = false;
        switch (treeCacheEvent.getType()) {
            case NODE_ADDED:
                if (initialized) {
                    this.printEventMsg(treeCacheEvent, true, true);
                    updated = true;
                }
                break;
            case NODE_UPDATED:
                if (initialized) {
                    this.printEventMsg(treeCacheEvent, true, true);
                    updated = true;
                }
                break;
            case NODE_REMOVED:
                if (initialized) {
                    this.printEventMsg(treeCacheEvent, true, false);
                    updated = true;
                }
                break;
            case INITIALIZED:
                initialized = true;
                updated = true;
                break;
            case CONNECTION_LOST:
            case CONNECTION_SUSPENDED:
            case CONNECTION_RECONNECTED:
                break;
            default:
                break;
        }
        if (updated) {
            // 刷新Discovery缓存
            ZooKeeperServiceDiscovery.subscribeAllServiceAddress();
        }
    }

    private void printEventMsg(TreeCacheEvent treeCacheEvent, boolean path, boolean data) {
        String msg = "[" + treeCacheEvent.getType() + "]";

        try {
            if (path) {
                String nodePath = treeCacheEvent.getData().getPath();
                msg += " 节点:" + nodePath;
            }
            if (data) {
                String nodeData = new String(treeCacheEvent.getData().getData(), CHARSET);
                msg += " 数据:" + nodeData;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        LOGGER.info(msg);
    }
}
