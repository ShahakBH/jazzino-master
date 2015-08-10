package com.yazino.novomatic.cgs.transport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;

public class ClientSocketConnection {
    private static final Logger LOG = LoggerFactory.getLogger(ClientSocketConnection.class);
    private final Socket socket;

    public ClientSocketConnection(String host, int port, int socketTimeoutInMillis) throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(socketTimeoutInMillis);
        LOG.debug("Created socket {}", socket);
    }

    public void send(byte[] payload) throws IOException {
        socket.getOutputStream().write(payload);
    }

    public byte[] receive(int payloadLength) throws IOException {
        final byte[] buffer = new byte[payloadLength];
        final int read = socket.getInputStream().read(buffer);
        LOG.debug("Read {} bytes", read);
        return buffer;
    }
}
