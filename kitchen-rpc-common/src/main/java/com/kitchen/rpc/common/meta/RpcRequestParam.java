package com.kitchen.rpc.common.meta;

/**
 * @author 赵梓彧 - kitchen_dev@163.com
 * @date 2017-07-05
 */
public class RpcRequestParam {
    private Class<?> paramType;
    private Object paramValue;

    public Class<?> getParamType() {
        return paramType;
    }

    public void setParamType(Class<?> paramType) {
        this.paramType = paramType;
    }

    public Object getParamValue() {
        return paramValue;
    }

    public void setParamValue(Object paramValue) {
        this.paramValue = paramValue;
    }
}
