package com.kitchen.rpc.registry.store;

/**
 * RPC服务注册器接口
 *
 * @date 2016-12-02
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public interface RpcServiceRegistry {

    /**
     * 注册服务节点信息
     *
     * @param rpcName RPC服务节点名称
     * @param rpcAddress RPC服务地址
     */
    void registerServiceNode(String rpcName, String rpcAddress);

    /**
     * 注册服务（名称、地址）
     *
     * @param serviceName    服务名称
     * @param serviceData 服务数据（地址等）
     */
    void registerService(String serviceName, String serviceData);

    /**
     * 关闭相关资源
     */
    void stop();
}
