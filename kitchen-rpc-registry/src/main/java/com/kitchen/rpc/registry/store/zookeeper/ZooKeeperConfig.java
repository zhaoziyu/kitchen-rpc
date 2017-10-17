package com.kitchen.rpc.registry.store.zookeeper;

/**
 * ZooKeeper配置
 *
 * @date 2016-12-03
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class ZooKeeperConfig {
    public static int ZK_SESSION_TIMEOUT = 5000;          //ZooKeeper 会话超时或过期设置
    public static int ZK_CONNECTION_TIMEOUT = 1000;       //ZooKeeper 请求超时设置

    /**
     * 注册中心目录结构，详见《注册中心节点结构图》
     */
    public static String ZK_FIX_PATH__PROVIDER_POOL = "/provider-pool";
    public static String ZK_FIX_PATH_CHAIN__NODES = ZK_FIX_PATH__PROVIDER_POOL + "/nodes";
    public static String ZK_TEMP_PATH__NODE_INFO = "/info-";

    public static String ZK_FIX_PATH_CHAIN__SERVICES = ZK_FIX_PATH__PROVIDER_POOL + "/services";

    public static String ZK_FIX_PATH__CUSTOMER_POOL = "/customer-pool";

    public static String ZK_TEMP_PATH__ADDRESS = "/address-";
}
