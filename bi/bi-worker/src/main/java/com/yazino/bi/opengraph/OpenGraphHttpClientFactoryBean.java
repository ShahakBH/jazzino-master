package com.yazino.bi.opengraph;


import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import strata.server.lobby.api.facebook.AbstractHttpClientFactoryBean;

@Component("openGraphHttpClient")
public class OpenGraphHttpClientFactoryBean extends AbstractHttpClientFactoryBean {

    private final int connectionTimeout;
    private final int socketTimeout;
    private final int maxConnections;

    @Autowired
    public OpenGraphHttpClientFactoryBean(@Value("${opengraph.publishing.connection-timeout}")
                                          final int connectionTimeout,
                                          @Value("${opengraph.publishing.socket-timeout}")
                                          final int socketTimeout,
                                          @Value("${opengraph.publishing.max-connections}")
                                          final int maxConnections) {
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        this.maxConnections = maxConnections;
    }

    @Override
    protected int getMaxConnectionsPerRoute() {
        return maxConnections;
    }

    @Override
    protected int getMaxConnectionsTotal() {
        return maxConnections;
    }

    @Override
    protected SocketConfig getSocketConfig() {
        return SocketConfig.custom()
                .setSoTimeout(socketTimeout)
                .build();
    }

    @Override
    protected RequestConfig getRequestConfig() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(connectionTimeout)
                .build();
    }
}
