package com.kitchen.rpc.client.handler;

import com.kitchen.rpc.client.RpcCallback;
import com.kitchen.rpc.client.future.RpcClientFuture;
import com.kitchen.rpc.client.cache.ClientChannelCache;
import com.kitchen.rpc.common.RequestMode;
import com.kitchen.rpc.common.exception.RpcChannelException;
import com.kitchen.rpc.common.meta.RpcRequest;
import com.kitchen.rpc.common.meta.RpcRequestParam;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * RPC客户端请求代理的处理器
 *
 * @date 2016-12-18
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class ClientProxyInvocationHandler implements InvocationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProxyInvocationHandler.class);
    /**
     * 所请求服务的版本号
     */
    private String serviceVersion;

    /**
     * 请求的类型
     */
    private RequestMode requestMode;

    private List<RpcCallback> callbacks;

    public ClientProxyInvocationHandler(String serviceVersion, RequestMode requestMode) {
        this.serviceVersion = serviceVersion;
        this.requestMode = requestMode;
    }

    public ClientProxyInvocationHandler(String serviceVersion, RequestMode requestMode, RpcCallback... callbacks) {
        this.serviceVersion = serviceVersion;
        this.requestMode = requestMode;
        this.callbacks = Arrays.asList(callbacks);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        Channel channel = null;
        try {
            String interfaceName = method.getDeclaringClass().getName();

            // 构建RPC请求对象，并设置请求属性
            RpcRequest request = new RpcRequest();
            request.setRequestId(UUID.randomUUID().toString());
            request.setRequestMode(requestMode);

            request.setInterfaceName(interfaceName);
            request.setServiceVersion(serviceVersion);
            request.setMethodName(method.getName());

            // 处理请求方法的参数
            HashMap<Integer, RpcRequestParam> paramHashMap = new HashMap<>();
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes != null) {
                for (int i = 0; i < parameterTypes.length; i++) {
                    RpcRequestParam rpcRequestParam = new RpcRequestParam();
                    rpcRequestParam.setParamType(parameterTypes[i]);
                    rpcRequestParam.setParamValue(args[i]);
                    paramHashMap.put(i, rpcRequestParam);
                }
            }
            request.setParameters(paramHashMap);

            // 获取Http请求对象
            // String sessionId = RequestContextHolder.currentRequestAttributes().getSessionId();
            HttpServletRequest httpServletRequest = null;
            try {
                httpServletRequest = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
            } catch (Exception e) {
                // 非HTTP环境的请求，无需处理
            }
            if (httpServletRequest != null) {
                String ip = httpServletRequest.getRemoteAddr();// 获取请求IP
                request.setRequestIp(ip);

                String sessionId = httpServletRequest.getRequestedSessionId();// 获取请求会话
                request.setRequestSessionId(sessionId);
            }

            // 从连接池获取连接
            channel = ClientChannelCache.getInstance().getChannel(interfaceName, serviceVersion, request);
            if (channel == null) {
                throw new RpcChannelException("在RPC通道连接池中获取连接失败");
            }

            if (requestMode == RequestMode.SYNC) {
                // 在通道中发送请求，并同步等待相应结果
                ClientChannelInboundHandler clientChannelInboundHandler = channel.pipeline().get(ClientChannelInboundHandler.class);
                RpcClientFuture future = clientChannelInboundHandler.sendRequest(channel, request);
                result = future.get();
                if (future.getResponse().hasException()) {
                    LOGGER.error("服务内抛出异常", future.getResponse().getException().getCause());
                }
            } else if (requestMode == RequestMode.ASYNC) {
                channel.writeAndFlush(request);
            } else if (requestMode == RequestMode.ASYNC_CALLBACK) {
                // 在通道中发送请求
                ClientChannelInboundHandler clientChannelInboundHandler = channel.pipeline().get(ClientChannelInboundHandler.class);
                clientChannelInboundHandler.sendRequest(channel, request, callbacks);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw e;
        } finally {
            // 释放连接到连接池
            if (requestMode != RequestMode.ASYNC_CALLBACK) {
                ClientChannelCache.getInstance().releaseChannel(channel);
            }
        }

        return result;
    }
}
