package com.yazino.yaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;

/**
 * A factory responsible for opening sockets to Apple.
 */
public class AppleSocketFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AppleSocketFactory.class);

    private final String mHost;
    private final int mPort;
    private final SecurityConfig mConfig;

    public AppleSocketFactory(final String host,
                              final int port,
                              final SecurityConfig config) {
        LOG.info("Configured with host [{}], port [{}], config [{}]", new Object[]{host, port, config});
        mHost = host;
        mPort = port;
        mConfig = config;
    }

    /**
     * Open a socket for the specified game.
     *
     * @return an new socket to this connections host
     * @throws IOException should we not be able to open a connection.
     */
    public Socket newSocket() throws IOException {
        LOG.debug("Creating SSL socket using certificate {}", mConfig.getCertificateName());
        SSLContext context = mConfig.getSSLContext();
        SSLSocketFactory factory = factoryFromContext(context);
        SSLSocket socket = (SSLSocket) factory.createSocket(mHost, mPort);
        socket.startHandshake();
        return socket;
    }

    SSLSocketFactory factoryFromContext(SSLContext context) {
        // like this for testing, getSocketFactory is final so can't be mocked
        return context.getSocketFactory();
    }

}
