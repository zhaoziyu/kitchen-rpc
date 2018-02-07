package com.kitchen.rpc.server.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * 处理RPC服务端启动相关参数
 *
 * Deprecated ：使用springboot默认机制，通过命令行传入的"--"开头的参数可直接覆盖配置属性。即：java -jar ***.jar --xxx.xxx.xxx=yyy，将覆盖application.yml中的xxx.xxx.xxx参数
 *
 * @author 赵梓彧 - zhaoziyu@inspur.com
 * @date 2017-07-03
 */
@Deprecated
public class LaunchUtil {

    // 运行参数缓存
    private static HashMap<String, String> launchArgs;

    /**
     * 入口方法，处理传入参数
     * @param args
     */
    public static void handlerArgs(String[] args) {
        List<String> argList = Arrays.asList(args);
        launchArgs = new HashMap<>();
        for (String arg : argList) {
            String[] splitArg = arg.split("=");
            String key = splitArg[0].toLowerCase();
            String value = splitArg[1].trim();
            launchArgs.put(key, value);
        }
    }

    /**
     * 是否存在某参数
     * @param argName
     * @return
     */
    public static boolean existArg(String argName) {
        if (launchArgs != null) {
            argName = argName.toLowerCase();
            return launchArgs.containsKey(argName);
        }
        return false;
    }

    /**
     * 获取启动参数值
     * 若无次参数，则返回null
     * @param argName
     * @return
     */
    public static String getArgValue(String argName) {
        if (launchArgs != null && existArg(argName)) {
            argName = argName.toLowerCase();
            return launchArgs.get(argName);
        }
        return null;
    }
}
