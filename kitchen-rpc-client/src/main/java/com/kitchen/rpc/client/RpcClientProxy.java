package com.kitchen.rpc.client;

import com.kitchen.rpc.client.handler.ClientProxyInvocationHandler;
import com.kitchen.rpc.common.RequestMode;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * RPC客户端请求代理类
 *
 * @date 2016-12-18
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class RpcClientProxy {

    /**
     * 创建指定服务接口的远程服务实例
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public static <T> T createSync(final Class<?> interfaceClass) {
        return createSync(interfaceClass, "");
    }

    /**
     * 创建指定服务接口和版本的远程服务实例
     * @param interfaceClass
     * @param serviceVersion
     * @param <T>
     * @return
     */
    public static <T> T createSync(final Class<?> interfaceClass, final String serviceVersion) {
        InvocationHandler proxyHandler = new ClientProxyInvocationHandler(serviceVersion, RequestMode.SYNC);
        return createObject(interfaceClass, proxyHandler);
    }

    /**
     * 创建指定服务接口的异步远程服务实例
     * @param interfaceClass
     * @param <T>
     * @return
     */
    public static <T> T createAsync(final Class<?> interfaceClass) {
        return createAsync(interfaceClass, "");
    }

    /**
     * 创建指定服务接口和版本的远程服务实例
     * @param interfaceClass
     * @param serviceVersion
     * @param <T>
     * @return
     */
    public static <T> T createAsync(final Class<?> interfaceClass, final String serviceVersion) {
        InvocationHandler proxyHandler = new ClientProxyInvocationHandler(serviceVersion, RequestMode.ASYNC);
        return createObject(interfaceClass, proxyHandler);
    }

    /**
     * 创建指定服务接口的异步远程服务实例，并设置回调处理
     * @param interfaceClass
     * @param callbacks
     * @param <T>
     * @return
     */
    public static <T> T createAsyncCallback(final Class<?> interfaceClass, RpcCallback... callbacks) {
        return createAsyncCallback(interfaceClass, "", callbacks);
    }

    /**
     * 创建指定服务接口和版本的异步远程服务实例，并设置回调处理
     * @param interfaceClass
     * @param serviceVersion
     * @param callbacks
     * @param <T>
     * @return
     */
    public static <T> T createAsyncCallback(final Class<?> interfaceClass, final String serviceVersion, RpcCallback... callbacks) {
        InvocationHandler proxyHandler = new ClientProxyInvocationHandler(serviceVersion, RequestMode.ASYNC_CALLBACK, callbacks);
        return createObject(interfaceClass, proxyHandler);
    }

    @SuppressWarnings("unchecked")
    private static  <T> T createObject(final Class<?> interfaceClass, InvocationHandler proxyHandler) {
        ClassLoader classLoader = interfaceClass.getClassLoader();
        Class<?>[] interfaceList = new Class<?>[]{
                interfaceClass
        };

        // 创建动态代理对象
        T object = (T) Proxy.newProxyInstance(
                classLoader,
                interfaceList,
                proxyHandler
        );

        return object;
    }

}
