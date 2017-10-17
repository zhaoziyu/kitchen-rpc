package com.kitchen.rpc.common.exception;

/**
 * <描述>
 *
 * @author 赵梓彧 - kitchen_dev@163.com
 * @date 2017-03-16
 */
public class RpcServiceException extends RuntimeException {
    public RpcServiceException(String message) {
        super(message);
    }
}
