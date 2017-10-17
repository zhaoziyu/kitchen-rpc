package com.kitchen.rpc.server.deploy.host.tool;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IP地址检查者，负责IP地址相关的验证和操作
 *
 * @date 2017-04-20
 * @author 赵梓彧 - kitchen_dev@163.com
 */
public class HostVerifier {
    /**
     * 验证是否为格式合法的IPv4地址
     * @param ip 需要验证的ip地址
     * @return 格式是否合法
     */
    public static boolean isIPv4(String ip) {
        if(ip.length() < 7 || ip.length() > 15 || "".equals(ip)) {
            return false;
        }
        String rexp =
                "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                        +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                        +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                        +"(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(ip);

        return mat.find();
    }

    /**
     * 从NetworkInterface列表中查找出所有IPv4的地址
     *
     * @param networkInterfaces
     * @return
     */
    public static List<String> findIPv4List(List<NetworkInterface> networkInterfaces) {
        List<String> matchingIpList = new ArrayList<>();
        List<InterfaceAddress> preselection = new ArrayList<>();
        for (NetworkInterface ni : networkInterfaces) {
            List<InterfaceAddress> addresses = ni.getInterfaceAddresses();
            for (InterfaceAddress address : addresses) {
                // 判断子网掩码
                if (address.getNetworkPrefixLength() != 32) {
                    String ip = address.getAddress().getHostAddress();
                    if (HostVerifier.isIPv4(ip)) {
                        preselection.add(address);
                    }
                }
            }
        }
        if (preselection.size() > 1) {
            for (InterfaceAddress address : preselection) {
                String hostName = address.getAddress().getHostName();
                String hostAddress = address.getAddress().getHostAddress();
                if (!hostName.equals(hostAddress)) {
                    // Docker Swarm网络会在eth0中绑定两个IP地址，一个是服务的虚地址，一个是实际地址，实际地址会和主机名（默认为容器ID）绑定，所以IP和主机名不一致，而虚地址的HostName和HostAddress是一致的
                    matchingIpList.add(hostAddress);
                }
            }
        } else if (preselection.size() == 1) {
            matchingIpList.add(preselection.get(0).getAddress().getHostAddress());
        }
        return matchingIpList;
    }
}
