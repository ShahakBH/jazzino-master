package strata.server.lobby.api.facebook;

import org.apache.http.HttpHost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("facebookHttpClient")
public class FacebookHttpClientFactoryBean extends AbstractHttpClientFactoryBean {
    public static final int MAX_FOR_FACEBOOK = 50;
    private static final int HTTPS_PORT = 443;

    @Override
    protected HttpClientConnectionManager getConnectionManager() {
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);
        connectionManager.setMaxTotal(MAX_TOTAL);
        final HttpHost facebookHost = new HttpHost("graph.facebook.com", HTTPS_PORT);
        connectionManager.setMaxPerRoute(new HttpRoute(facebookHost), MAX_FOR_FACEBOOK);
        return connectionManager;
    }

}
