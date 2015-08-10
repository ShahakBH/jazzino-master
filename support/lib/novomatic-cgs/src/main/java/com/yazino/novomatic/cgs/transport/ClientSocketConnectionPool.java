package com.yazino.novomatic.cgs.transport;

import com.yazino.configuration.YazinoConfiguration;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClientSocketConnectionPool extends GenericObjectPool<ClientSocketConnection> {

    @Autowired
    public ClientSocketConnectionPool(YazinoConfiguration yazinoConfiguration) {
        super(new ClientSocketConnectionFactory(yazinoConfiguration), yazinoConfiguration.getInt("novomatic.cgs.max-connection"));
    }
}
