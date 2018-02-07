package com.kitchen.rpc.server;

import org.springframework.stereotype.Service;
/**
 * RPC服务注解（标注在RPC服务的实现类上）
 * RPC服务端扫描带有此注解的服务实现，并将服务暴露出来
 *
 * @date 2016-12-02
 * @author 赵梓彧 - kitchen_dev@163.com
 */
@Service
public @interface RpcService {
    /**
     * 服务接口类
     */
    //Class<?> value();

    /**
     * 服务版本号
     */
    String version() default "";
}
