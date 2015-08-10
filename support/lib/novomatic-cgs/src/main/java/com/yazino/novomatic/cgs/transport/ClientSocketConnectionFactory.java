package com.yazino.novomatic.cgs.transport;

import com.yazino.configuration.YazinoConfiguration;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSocketConnectionFactory extends BasePoolableObjectFactory<ClientSocketConnection> {
    private static final Logger LOG = LoggerFactory.getLogger(ClientSocketConnectionFactory.class);
    private final YazinoConfiguration yazinoConfiguration;

    public ClientSocketConnectionFactory(YazinoConfiguration yazinoConfiguration) {
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Override
    public ClientSocketConnection makeObject() throws Exception {
        final String host = yazinoConfiguration.getString("novomatic.cgs.host");
        final int port = yazinoConfiguration.getInt("novomatic.cgs.port");
        final int socketTimeoutInMillis = yazinoConfiguration.getInt("novomatic.cgs.socket-timeout", 5000);
        LOG.info("Creating connection to Novomatic CGS (host={}, port={}, socketTimeout={})", host, port, socketTimeoutInMillis);
        return new ClientSocketConnection(host, port, socketTimeoutInMillis);
    }


}
