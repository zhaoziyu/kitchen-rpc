package com.kitchen.rpc.server.deploy.host;

import com.kitchen.rpc.common.exception.ProviderDeployException;

/**
 * 查找宿主主机ip的接口
 *
 * @date 2017-04-20
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public interface HostFetcher {

    /**
     * 获取可用的IP地址
     *
     * @param hostArg 对应配置中的kitchen.rpc.server.host参数
     * @return
     */
    String getIp(String hostArg) throws ProviderDeployException;

}
