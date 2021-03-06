package com.kitchen.rpc.registry.store.zookeeper.discovery;

import com.kitchen.rpc.registry.store.zookeeper.CommonCuratorTool;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * ZooKeeper发现者的客户端
 *
 * @author 赵梓彧 - kitchen_dev@163.com
 * @date 2017-03-15
 */
public class DiscoveryCurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryCurator.class);

    //------------------------------------------------------CuratorFramework-----------------------------------------------------
    /**
     * ZooKeeper客户端
     */
    private static CuratorFramework ZOOKEEPER_CLIENT;

    /**
     * 创建ZooKeeper客户端，并开启与ZooKeeper服务器的连接
     *
     * @param zooKeeperAddress ZooKeeper服务器地址，格式：ip:port
     */
    public static void connectionZooKeeperServer(String zooKeeperAddress, RetryPolicy retryPolicy) {
        // 创建 ZooKeeper 客户端
        ZOOKEEPER_CLIENT = CommonCuratorTool.connectionZooKeeperServer(zooKeeperAddress, retryPolicy);
        ZOOKEEPER_CLIENT.start();
    }
    /**
     * 添加ZooKeeper连接状态监听器
     *
     * @param connectionStateListener 监听器
     */
    public static void addConnectionStateListener(ConnectionStateListener connectionStateListener) {
        // 添加链接状态监听器
        CommonCuratorTool.addConnectionStateListener(ZOOKEEPER_CLIENT, connectionStateListener);
    }
    /**
     * 断开ZooKeeper的客户端与服务器的连接
     */
    public static void disconnectionZooKeeperServer() {
        CommonCuratorTool.disconnectionZooKeeperServer(ZOOKEEPER_CLIENT);
        LOGGER.info("<RpcClient>: 已关闭ZooKeeper连接");
    }
    //------------------------------------------------------CuratorFramework-----------------------------------------------------

    //---------------------------------------------------------TreeCache---------------------------------------------------------
    /**
     * ZooKeeper节点的本地缓存及节点状态监听
     */
    private static TreeCache TREE_NODES_CACHE;

    /**
     * 创建ZooKeeper的TreeCache
     *
     * @param path
     */
    public static void createTreeNodeCache(String path, TreeCacheListener listener) {
        TREE_NODES_CACHE = new TreeCache(ZOOKEEPER_CLIENT, path);
        TREE_NODES_CACHE.getListenable().addListener(listener);
        try {
            TREE_NODES_CACHE.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 销毁ZooKeeper的TreeCache
     */
    public static void destroyTreeNodeCache() {
        if (TREE_NODES_CACHE != null) {
            TREE_NODES_CACHE.close();
            TREE_NODES_CACHE = null;
            LOGGER.info("<RpcClient>: 关闭ZooKeeper节点检查者");
        }
    }
    //---------------------------------------------------------TreeCache---------------------------------------------------------



    /**
     * 检查节点是否存在
     */
    public static boolean checkNodeExist(String path) {
        try {
            return CommonCuratorTool.checkNodeExist(ZOOKEEPER_CLIENT, path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取某个节点下的子节点
     *
     * @param path
     * @return
     */
    public static List<String> getChildrenNode(String path) {
        List<String> childrenNodes = new ArrayList<>();
        try {
            childrenNodes = CommonCuratorTool.getChildrenNode(ZOOKEEPER_CLIENT, path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return childrenNodes;
    }

    /**
     * 获取某个节点下的数据
     *
     * @param path
     * @return
     */
    public static String getNodeData(String path) {
        try {
            return CommonCuratorTool.getNodeData(ZOOKEEPER_CLIENT, path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建永久节点
     * 创建节点后，不删除就永久存在
     */
    public static void createFixNode(String path) {
        try {
            CommonCuratorTool.createFixNode(ZOOKEEPER_CLIENT, path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建临时序列节点
     * 创建后，节点path末尾会追加一个10位数的单调递增的序列，会话结束节点会自动删除
     */
    public static String createTempSeqNode(String path) {
        try {
            return CommonCuratorTool.createTempSeqNode(ZOOKEEPER_CLIENT, path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建临时序列节点
     * 创建后，节点path末尾会追加一个10位数的单调递增的序列，会话结束节点会自动删除
     */
    public static String createTempSeqNode(String path, String content) {
        try {
            return CommonCuratorTool.createTempSeqNode(ZOOKEEPER_CLIENT, path, content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
