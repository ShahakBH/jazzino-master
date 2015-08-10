package com.yazino.novomatic.cgs.transport;

import org.apache.commons.pool.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

@Component
public class ClientTransport {

    private static final Logger LOG = LoggerFactory.getLogger(ClientTransport.class);

    private ObjectPool<ClientSocketConnection> pool;

    @Autowired
    public ClientTransport(final ObjectPool<ClientSocketConnection> pool) {
        this.pool = pool;
    }

    public byte[] sendRequest(byte[] request) {
        LOG.debug("Request length is: {}", request.length);
        ClientSocketConnection socket = null;
        try {
            socket = pool.borrowObject();
            return sendRequest(socket, request);
        } catch (Exception e) {
            LOG.error("Error handling request. Invalidating the connection", e);
            try {
                pool.invalidateObject(socket);
                socket = null;
            } catch (Exception e1) {
                LOG.error("Error invalidating connection", e1);
            }
            throw new RuntimeException("Unable to send request", e);
        } finally {
            if (socket != null) {
                try {
                    pool.returnObject(socket);
                } catch (Exception e) {
                    LOG.error("Error returning connection to the pool", e);
                }
            }
        }
    }

    private byte[] sendRequest(ClientSocketConnection socket, byte[] request) throws IOException {
        byte[] requestLengthRaw = ByteBuffer.allocate(4).putInt(request.length).array();
        socket.send(requestLengthRaw);
        LOG.debug("Sending request: {}", packetAsString(request));
        socket.send(request);
        LOG.debug("Waiting for response");
        final byte[] responseLengthRaw = socket.receive(4);
        final int responseLength = ByteBuffer.wrap(responseLengthRaw).getInt();
        LOG.debug("Response length is {}", responseLength);
        final byte[] response = socket.receive(responseLength);
        LOG.debug("Received response: {}", packetAsString(response));
        return response;
    }

    private String packetAsString(byte[] packet) {
        return Arrays.toString(toUnsignedBytes(packet));
    }

    private int[] toUnsignedBytes(byte[] bs) {
        int[] res = new int[bs.length];
        for (int i = 0; i < bs.length; i++) {
            res[i] = unsignedToBytes(bs[i]);
        }
        return res;
    }

    private int unsignedToBytes(byte b) {
        return b & 0xFF;
    }
}
