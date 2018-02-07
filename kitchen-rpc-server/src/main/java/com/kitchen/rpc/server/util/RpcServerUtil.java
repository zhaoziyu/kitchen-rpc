package com.kitchen.rpc.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * RPC服务器的Spring Context工具类
 *
 * @date 2016-12-27
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class RpcServerUtil {
    private static final Logger logger = LoggerFactory.getLogger(RpcServerUtil.class);
    private static ApplicationContext context;

    public static void setContext(ApplicationContext context) {
        RpcServerUtil.context = context;

        List<String> frame = new ArrayList<>();
        List<String> business = new ArrayList<>();
        for (String name : context.getBeanDefinitionNames()) {
            if (name.indexOf(".") > 0) {
                frame.add(name);
            } else {
                business.add(name);
            }
        }

        String log = "";
        log += "\n通过Spring扫描加载下列类：";
        log += "\n-框架相关-";
        for (String name : frame) {
            log += "\n" + name;
        }
        log += "\n-业务相关-";
        for (String name : business) {
            log += "\n" + name;
        }
        log += "\n——————————————————————";
        logger.debug(log);
    }

    public static <T> T getBean(final Class<?> loadClass) throws Exception {
        String name = "";
        // TODO 通过类名获取@Service注解中的名称，若无则获取首字母小写的类名，再通过名称从Spring中获取bean
        throw new Exception("TODO");
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String loadClass) throws ClassCastException {
        T obj;
        try {
            obj = (T) context.getBean(loadClass);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClassCastException("泛型中的对象类型错误");
        }
        return obj;
    }
}
