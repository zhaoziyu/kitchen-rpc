package com.kitchen.rpc.registry;

/**
 * RPC客户端的请求类型（方式）
 *
 * @date 2016-12-21
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public enum RegistryMode {
    // 固定地址的服务提供者
    FIX,

    // 使用ZooKeeper管理服务注册和发现
    ZOOKEEPER
}