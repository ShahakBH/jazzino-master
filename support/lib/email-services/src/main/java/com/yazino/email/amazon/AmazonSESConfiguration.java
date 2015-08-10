package com.yazino.email.amazon;

import com.amazonaws.ClientConfiguration;

public class AmazonSESConfiguration {

    private int socketTimeout = ClientConfiguration.DEFAULT_SOCKET_TIMEOUT;
    private int connectionTimeout = ClientConfiguration.DEFAULT_SOCKET_TIMEOUT;

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(final int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(final int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public ClientConfiguration asConfiguration() {
        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setConnectionTimeout(connectionTimeout);
        clientConfiguration.setSocketTimeout(socketTimeout);
        return clientConfiguration;
    }

}
