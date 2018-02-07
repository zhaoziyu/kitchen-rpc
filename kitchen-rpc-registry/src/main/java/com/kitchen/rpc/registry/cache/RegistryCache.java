package com.kitchen.rpc.registry.cache;

import com.kitchen.rpc.registry.store.RpcServiceDiscovery;
import com.kitchen.rpc.registry.store.RpcServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC服务注册地址缓存类
 * 存放（缓存）服务提供者内部的“服务名”与“服务对象”之间的映射关系
 *
 * @author 赵梓彧 - kitchen_dev@163.com
 * @date 2016-12-03
 */
public class RegistryCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryCache.class);

    private static String rpcServiceName = "";
    private static String serverAddress = "";
    private static Integer serverWeight = 1;
    private static Map<String, Object> handlerMap = new HashMap<>();

    private static RpcServiceRegistry RPC_SERVICE_REGISTRY;


    public static String getRpcServiceName() {
        return rpcServiceName;
    }

    public static void setRpcServiceName(String rpcServiceName) {
        RegistryCache.rpcServiceName = rpcServiceName;
    }

    public static void put(String key, Object value) {
        handlerMap.put(key, value);
    }

    public static Map<String, Object> get() {
        return handlerMap;
    }

    public static Object get(String key) {
        return handlerMap.get(key);
    }

    public static void address(String address) {
        serverAddress = address;
    }

    public static String address() {
        return serverAddress;
    }

    public static void weight(Integer weigth) {
        serverWeight = weigth;
    }

    public static Integer weight() {
        return serverWeight;
    }

    /**
     * 注册缓存中的 RPC 服务地址
     */
    public static void registryCacheService(RpcServiceRegistry rpcServiceRegistry) {
        if (rpcServiceRegistry != null) {
            RPC_SERVICE_REGISTRY = rpcServiceRegistry;
            String log = "<RpcServer>: 注册服务——>\n";
            for (String interfaceName : handlerMap.keySet()) {
                // 注册服务的数据：“ip:port|权重”
                // TODO 改成存储JSON格式
                String content = serverAddress + "|" + serverWeight;

                // 注册服务
                rpcServiceRegistry.registerService(interfaceName, content);
                log += "服务提供者(" + rpcServiceName + "  " + serverAddress + ")注册服务[" + interfaceName + "]至服务注册中心\n";
            }

            // 注册服务节点信息
            rpcServiceRegistry.registerServiceNode(rpcServiceName, serverAddress);
            log += "服务提供者(" + rpcServiceName + "  " + serverAddress + ")完成全部服务注册\n";
            LOGGER.info(log);
        }
    }

    public static void destroy() {
        if (RPC_SERVICE_REGISTRY != null) {
            RPC_SERVICE_REGISTRY.stop();
        }
    }
}
