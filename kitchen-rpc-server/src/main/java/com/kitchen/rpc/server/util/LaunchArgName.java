package com.kitchen.rpc.server.util;

/**
 * @author 赵梓彧 - zhaoziyu@inspur.com
 * @date 2017-07-03
 */
public class LaunchArgName {
    // 参数：服务注册中心地址
    public final static String ARG__REGISTRY = "-Registry";

    // 参数：指定所部署主机IP的获取方式
    public final static String ARG__SERVER_HOST_TYPE = "-HostType";
    // 参数：RPC服务提供者的IP地址
    public final static String ARG__SERVER_HOST = "-Host";

    // 参数：指定所部署主机端口的获取方式
    public final static String ARG__SERVER_PORT_TYPE = "-PortType";
    // 参数：RPC服务提供者的端口
    public final static String ARG__SERVER_PORT = "-Port";
}
