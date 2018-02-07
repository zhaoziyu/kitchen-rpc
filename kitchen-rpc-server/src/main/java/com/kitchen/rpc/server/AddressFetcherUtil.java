package com.kitchen.rpc.server;

import com.kitchen.rpc.common.exception.ProviderDeployException;
import com.kitchen.rpc.server.deploy.ServerDeployFetcherFactory;
import com.kitchen.rpc.server.deploy.host.HostFetcher;
import com.kitchen.rpc.server.deploy.port.PortFetcher;

class AddressFetcherUtil {
    protected static String getHostIp(String serverHostType, String serverHost) {
        HostFetcher hostFetcher = ServerDeployFetcherFactory.createHostFetcher(serverHostType);
        String ip = null;
        if (hostFetcher != null) {
            try {
                ip = hostFetcher.getIp(serverHost);
            } catch (ProviderDeployException e) {
                e.printStackTrace();
            }
        }
        return ip;
    }
    protected static Integer getDeployPort(String serverPortType, Integer serverPort) {
        PortFetcher portFetcher = ServerDeployFetcherFactory.createPortFetcher(serverPortType);
        Integer port = null;
        if (portFetcher != null) {
            try {
                port = portFetcher.getPort(serverPort);
            } catch (ProviderDeployException e) {
                e.printStackTrace();
            }
        }
        return port;
    }
}
