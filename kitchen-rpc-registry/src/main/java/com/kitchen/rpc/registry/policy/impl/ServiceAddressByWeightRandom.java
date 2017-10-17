package com.kitchen.rpc.registry.policy.impl;

import com.kitchen.rpc.common.meta.RpcRequest;
import com.kitchen.rpc.registry.policy.BaseServiceAddressPolicy;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 负载均衡策略：加权随机（Weight Random）
 * 与加权轮询法一样，加权随机法也根据后端机器的配置，系统的负载分配不同的权重。不同的是，它是按照权重随机请求后端服务器，而非顺序。
 *
 * @author 赵梓彧 - kitchen_dev@163.com
 * @date 2017-03-16
 */
public class ServiceAddressByWeightRandom implements BaseServiceAddressPolicy {
    @Override
    public String getAddress(LinkedHashMap<String, Integer> addressMap, RpcRequest rpcRequest) {
        // 取得IP地址列表
        List<String> serverList = new ArrayList<>();
        for (Map.Entry<String,Integer> item : addressMap.entrySet()) {
            int weight = item.getValue();
            for (int i = 0; i < weight; i++) {
                serverList.add(item.getKey());
            }
        }

        String address;
        int size = serverList.size();
        if (size == 1) {
            address = serverList.get(0);
        } else {
            address = serverList.get(ThreadLocalRandom.current().nextInt(size));
        }
        return address;
    }
}
