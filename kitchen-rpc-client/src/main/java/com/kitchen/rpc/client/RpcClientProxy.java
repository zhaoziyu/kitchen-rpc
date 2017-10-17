package com.kitchen.rpc.client;

import com.kitchen.rpc.client.cache.ClientChannelCache;
import com.kitchen.rpc.client.handler.ClientProxyInvocationHandler;
import com.kitchen.rpc.client.thread.CallbackThreadPool;
import com.kitchen.rpc.common.RequestMode;
import com.kitchen.rpc.registry.cache.DiscoveryCache;
import com.kitchen.rpc.registry.policy.PolicyConfig;
import com.kitchen.rpc.registry.store.RpcServiceDiscovery;
import com.kitchen.rpc.registry.policy.LoadBalancePolicyType;
import com.kitchen.rpc.registry.store.fix.FixStoreConfig;
import com.kitchen.rpc.registry.store.zookeeper.discovery.ZooKeeperServiceDiscovery;
import io.netty.util.ThreadDeathWatcher;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * RPC客户端请求代理类
 *
 * @date 2016-12-18
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class RpcClientProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientProxy.class);
    private static boolean OPEN_RPC = true;

    /**
     * 设置固定的服务提供者地址，使用此方式将不会再服务注册中心查找
     *
     * @param serviceAddress
     */
    public RpcClientProxy(String[] serviceAddress) throws Exception {
        OPEN_RPC = false;
        PolicyConfig.GET_SERVICE_ADDRESS_POLICY = LoadBalancePolicyType.Random;

        // 将固定地址写入服务发现缓存，以供客户端查找服务地址时调用
        Map<String, LinkedHashMap<String, Integer>> addressMap = new ConcurrentHashMap<>();
        LinkedHashMap<String, Integer> address = new LinkedHashMap<>();
        for (String addr : serviceAddress) {
            address.put(addr, 1);// 默认权重1
        }
        addressMap.put(FixStoreConfig.DEFAULT_KEY, address);
        DiscoveryCache.setServiceAddressCache(addressMap);
    }

    /**
     * 初始化注册中心，在注册中心查找服务提供者
     * @param registryCenterAddress
     */
    public RpcClientProxy(String registryCenterAddress, boolean open, String name, LoadBalancePolicyType policyType) {
        // 设置RPC打开状态
        OPEN_RPC = open;
        // 设置服务调用的负载均衡策略
        PolicyConfig.GET_SERVICE_ADDRESS_POLICY = policyType;
        if (OPEN_RPC) {
            // TODO 注册和发现通过“配置+抽象工厂”实现，支持多种注册中心
            RpcServiceDiscovery rpcServiceDiscovery = new ZooKeeperServiceDiscovery(registryCenterAddress, name);
            ClientChannelCache.setRpcServiceDiscovery(rpcServiceDiscovery);
        } else {
            LOGGER.info("未启用RPC服务，如需启用RPC服务，请在rpc-consumer.properties中设置rpc.consumer.open为true");
        }
    }

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

    /**
     * 是否开启RPC调用方式
     * @return
     */
    public static boolean isOpenRpc() {
        return OPEN_RPC;
    }

    private void destroy() {
        if (RpcClientProxy.isOpenRpc()) {
            LOGGER.info("回收RPC相关资源");
            if (ClientChannelCache.getRpcServiceDiscovery() != null) {
                ClientChannelCache.getRpcServiceDiscovery().stop();
            }
            if (ClientChannelCache.isRuning()) {
                ClientChannelCache.getInstance().stop();
            }
        }
        CallbackThreadPool.stop();

        try {
            GlobalEventExecutor.INSTANCE.awaitInactivity(5L, TimeUnit.SECONDS);
            ThreadDeathWatcher.awaitInactivity(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }

        /*
        System.out.println("正在清理相关资源");
        for (int i = 5; i > 0; i--) {
            try {
                Thread.sleep(1000);
                System.out.println("倒计时：" + i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("资源清理完毕");
        */
    }
}
