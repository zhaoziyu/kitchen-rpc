package com.kitchen.rpc.registry.cache;

import com.kitchen.rpc.common.meta.RpcRequest;
import com.kitchen.rpc.registry.policy.PolicyConfig;
import com.kitchen.rpc.registry.policy.BaseServiceAddressPolicy;
import com.kitchen.rpc.registry.policy.impl.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务探寻器缓存
 *
 * @date 2016-12-06
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class DiscoveryCache {

    // 客户端发现的服务地址缓存至内存
    private static Map<String, LinkedHashMap<String, Integer>> SERVICE_ADDRESS_CACHE = new ConcurrentHashMap<>();

    private static BaseServiceAddressPolicy policy;

    static {
        switch (PolicyConfig.SERVICE_LB_POLICY) {
            case Random:
                // 适用场景不限
                policy = new ServiceAddressByRandom();
                break;
            case RoundRobin:
                // 适用场景：每个服务提供者都提供相同的服务菜单
                policy = new ServiceAddressByRoundRobin();
                break;
            case WeightRandom:
                // 适用场景不限
                policy = new ServiceAddressByWeightRandom();
                break;
            case WeightRoundRobin:
                // 适用场景：每个服务提供者都提供相同的服务菜单
                policy = new ServiceAddressByWeightRoundRobin();
                break;
            case IpAddressHash:
                // 适用场景：要求请求会话一致的业务
                policy = new ServiceAddressByHash();
                break;
            default:
                policy = new ServiceAddressByRandom();
                break;
        }
    }

    /**
     * 获取服务的访问地址（URL）
     * @param serviceName
     * @return
     */
    public static String getServiceAddress(String serviceName, RpcRequest rpcRequest) {
        if (SERVICE_ADDRESS_CACHE == null) {
            return null;
        }
        String address = null;
        // 深拷贝
        if (SERVICE_ADDRESS_CACHE.containsKey(serviceName)) {
            LinkedHashMap<String, Integer> addressMap = (LinkedHashMap<String, Integer>) SERVICE_ADDRESS_CACHE.get(serviceName).clone();
            address = policy.getAddress(addressMap, rpcRequest);
        }

        return address;
    }

    public static void setServiceAddressCache(Map<String, LinkedHashMap<String, Integer>> newCache) {
        synchronized (SERVICE_ADDRESS_CACHE) {
            SERVICE_ADDRESS_CACHE.clear();
            SERVICE_ADDRESS_CACHE.putAll(newCache);
        }
    }

    public static void clear() {
        SERVICE_ADDRESS_CACHE.clear();
    }
}
