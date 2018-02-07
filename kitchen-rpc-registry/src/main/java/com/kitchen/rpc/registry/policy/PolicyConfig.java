package com.kitchen.rpc.registry.policy;

/**
 * @author 赵梓彧 - kitchen_dev@163.com
 * @date 2017-09-05
 */
public class PolicyConfig {
    /**
     * 获取服务地址时的负载均衡策略
     */
    public static LoadBalancePolicyType SERVICE_LB_POLICY = LoadBalancePolicyType.RoundRobin;
}
