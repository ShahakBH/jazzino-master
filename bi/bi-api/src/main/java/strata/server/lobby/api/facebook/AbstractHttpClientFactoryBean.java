package strata.server.lobby.api.facebook;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public abstract class AbstractHttpClientFactoryBean extends AbstractFactoryBean<CloseableHttpClient> {

    public static final int CONNECTION_TIMEOUT_VALUE = 1000;
    public static final int DEFAULT_MAX_PER_ROUTE = 20;
    public static final int MAX_TOTAL = 50;

    @Override
    public Class<?> getObjectType() {
        return HttpClient.class;
    }

    @Override
    protected CloseableHttpClient createInstance() throws Exception {
        HttpClientBuilder builder = HttpClientBuilder.create()
                .setDefaultRequestConfig(getRequestConfig())
                .setDefaultSocketConfig(getSocketConfig())
                .setMaxConnPerRoute(getMaxConnectionsPerRoute())
                .setMaxConnTotal(getMaxConnectionsTotal());

        final HttpClientConnectionManager connectionManager = getConnectionManager();
        if (connectionManager != null) {
            builder = builder.setConnectionManager(connectionManager);
        }

        return builder.build();
    }

    @Override
    protected void destroyInstance(final CloseableHttpClient instance) throws Exception {
        instance.close();
    }

    protected int getMaxConnectionsPerRoute() {
        return DEFAULT_MAX_PER_ROUTE;
    }

    protected int getMaxConnectionsTotal() {
        return MAX_TOTAL;
    }

    protected SocketConfig getSocketConfig() {
        return SocketConfig.custom()
                .build();
    }

    protected RequestConfig getRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT_VALUE)
                .build();
    }

    protected HttpClientConnectionManager getConnectionManager() {
        return null;
    }
}
