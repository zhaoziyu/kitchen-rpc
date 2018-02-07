package com.kitchen.rpc.registry.store.zookeeper.discovery;

import com.kitchen.rpc.common.meta.RpcRequest;
import com.kitchen.rpc.registry.policy.LoadBalancePolicyType;
import com.kitchen.rpc.registry.policy.PolicyConfig;
import com.kitchen.rpc.registry.store.RpcServiceDiscovery;
import com.kitchen.rpc.registry.cache.DiscoveryCache;
import com.kitchen.rpc.registry.store.zookeeper.ZooKeeperConfig;
import org.apache.curator.RetryPolicy;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 基于 ZooKeeper 的服务发现接口实现
 *
 * @date 2016-12-03
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class ZooKeeperServiceDiscovery implements RpcServiceDiscovery {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperServiceDiscovery.class);
    static String registryCenterAddress;

    static String systemName;

    public ZooKeeperServiceDiscovery(String address, String name) {
        registryCenterAddress = address;
        systemName = name;
        try {
            initZooKeeperClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String discoverService(String serviceName, RpcRequest rpcRequest) {
        String serviceAddress = DiscoveryCache.getServiceAddress(serviceName, rpcRequest);

        return serviceAddress;
    }

    @Override
    public void stop() {
        // 关闭ZooKeeper节点检查者
        DiscoveryCurator.destroyProviderNodeCache();
        // 清空服务地址缓存
        DiscoveryCache.clear();
        // 关闭ZooKeeper连接
        DiscoveryCurator.disconnectionZooKeeperServer();
    }

    /**
     * ZooKeeper初始化
     *
     * @throws InterruptedException
     */
    public static void initZooKeeperClient() throws InterruptedException {
        LOGGER.info("连接ZooKeeper目录服务器中......");

        // 创建 ZooKeeper 客户端
        //RetryPolicy retryPolicy = new RetryUntilElapsed(1000 * 60 * 10, 1000); // 尝试重连10分钟，每秒一次
        RetryPolicy retryPolicy = new RetryNTimes(Integer.MAX_VALUE, 10000); // 尝试Integer.MAX_VALUE次重连，每10秒尝试一次
        DiscoveryCurator.connectionZooKeeperServer(registryCenterAddress, retryPolicy);

        DiscoveryConnectionStateListener connectionStateListener = new DiscoveryConnectionStateListener();
        DiscoveryCurator.addConnectionStateListener(connectionStateListener);

        // 注册服务消费者的信息
        registerCustomerNode(systemName, "TODO: dynamic ip address." + UUID.randomUUID().toString().replace("-", "").toUpperCase());
    }

    /**
     * 注册服务消费者的信息
     *
     * @param customerName 发现者名称（通常为系统名）
     * @param customerAddress 发现者地址（通常用于更高一层的负载均衡使用）
     */
    private static void registerCustomerNode(String customerName, String customerAddress) {
        // 创建 service 节点（持久）
        String servicePath = ZooKeeperConfig.ZK_FIX_PATH__CUSTOMER_POOL + "/" + customerName;
        if (!DiscoveryCurator.checkNodeExist(servicePath)) {
            DiscoveryCurator.createFixNode(servicePath);
        }
        // 创建 address 节点（临时）
        String addressPath = servicePath + ZooKeeperConfig.ZK_TEMP_PATH__ADDRESS;
        DiscoveryCurator.createTempSeqNode(addressPath, customerAddress);
    }

    /**
     * 订阅全部服务即服务提供者的访问地址
     */
    public static void subscribeAllServiceAddress() {
        String log = "";
        log += "订阅服务";
        Map<String, LinkedHashMap<String, Integer>> tempServiceAddressCache = new ConcurrentHashMap<>();
        try {
            String registryPath = ZooKeeperConfig.ZK_FIX_PATH_CHAIN__SERVICES;
            if (!DiscoveryCurator.checkNodeExist(registryPath)) {
                DiscoveryCurator.createFixNode(registryPath);
            }

            // 从ZooKeeper中获取服务列表
            List<String> servicePathList = DiscoveryCurator.getChildrenNode(registryPath);
            for (String servicePath : servicePathList) {
                List<String> addressPathList = DiscoveryCurator.getChildrenNode(registryPath + "/" + servicePath);
                LinkedHashMap<String, Integer> addressMap = new LinkedHashMap<>();
                for (String addressPath : addressPathList) {
                    String path = registryPath + "/" + servicePath + "/" + addressPath;
                    String data = DiscoveryCurator.getNodeData(path);
                    if (data != null) {
                        // 服务目录中保存的格式为：ip:port|权重
                        String[] arrData = data.split("\\|");
                        addressMap.put(arrData[0], Integer.parseInt(arrData[1]));
                    }
                }
                tempServiceAddressCache.put(servicePath, addressMap);

                // 输出订阅信息
                log += "\n订阅[" + servicePath + "]服务";
                Iterator iterator = addressMap.entrySet().iterator();
                boolean hasAddress = false;
                String innerLog = "";
                while (iterator.hasNext()) {
                    hasAddress = true;
                    Map.Entry entry = (Map.Entry) iterator.next();
                    innerLog += "\n    地址[" + entry.getKey() + "] 权重[" + entry.getValue() + "]";
                }
                if (hasAddress) {
                    log += " 可用服务地址：";
                } else {
                    log += " 未发现可用服务地址";
                }

                log += innerLog;
            }
            DiscoveryCache.setServiceAddressCache(tempServiceAddressCache);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        LOGGER.info(log);
    }

    /**
     * 监听path路径下的所有子节点
     */
    public static void watcherPath(String path) {
        // 创建节点缓存
        DiscoveryCurator.createProviderNodeCache(path);

        // 添加节点监听器
        DiscoveryCurator.addProviderNodeListener(new DiscoveryTreeCacheListener());
    }
}
