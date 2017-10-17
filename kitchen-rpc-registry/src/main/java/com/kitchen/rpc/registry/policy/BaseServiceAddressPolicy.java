package com.kitchen.rpc.registry.policy;

import com.kitchen.rpc.common.meta.RpcRequest;

import java.util.LinkedHashMap;

/**
 * 获取服务提供者注册地址的策略
 * 定义同一个服务注册了多个访问者时，发现者在获取服务地址时所采用的策略
 * 默认：随机获取一个地址
 *
 * @author 赵梓彧 - kitchen_dev@163.com
 * @date 2017-03-16
 */
public interface BaseServiceAddressPolicy {
    /**
     * 获取传入参数中的一个地址，并返回
     *
     * @param addressMap key:服务器地址 value:服务器权重
     * @return
     */
    String getAddress(LinkedHashMap<String, Integer> addressMap, RpcRequest rpcRequest);
}
