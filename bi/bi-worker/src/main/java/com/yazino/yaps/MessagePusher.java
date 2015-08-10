package com.yazino.yaps;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Manages the writing and reading of a {@link TargetedMessage} to a socket.
 */
public class MessagePusher {
    private static final Logger LOG = LoggerFactory.getLogger(MessagePusher.class);

    private PushResponseTransformer mResponseTransformer = new PushResponseTransformer();
    private TargetedMessageTransformer mMessageTransformer = new TargetedMessageTransformer();
    private int mSocketReadTimeout = 100;

    public PushResponse pushAndReadResponse(AppleConnection connection, TargetedMessage message) throws Exception {
        Socket socket = connection.getSocket();
        writeMessage(socket, message);
        socket.setSoTimeout(mSocketReadTimeout);
        return readResponse(socket);
    }

    private void writeMessage(Socket socket, TargetedMessage message) throws IOException, MessageTransformationException {
        final byte[] bytes = mMessageTransformer.toBytes(message);
        final OutputStream outputStream = socket.getOutputStream();
        outputStream.write(bytes);
        outputStream.flush();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Wrote message [%s]", message));
        }
    }

    private PushResponse readResponse(Socket socket) throws IOException, MessageTransformationException {
        final InputStream inputStream = socket.getInputStream();
        final byte[] bytes = new byte[6];
        try {
            final int read = inputStream.read(bytes);
            if (read > 0) {
                final PushResponse response = mResponseTransformer.fromBytes(bytes);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Read response [%s] from socket.", response));
                }
                return response;
            }
        } catch (SocketTimeoutException e) {
            // normal execution, nothing to read
            if (LOG.isTraceEnabled()) {
                LOG.trace("Timed out waiting for input on socket.");
            }
        }
        return PushResponse.OK;
    }

    public void setResponseTransformer(final PushResponseTransformer responseTransformer) {
        notNull(responseTransformer, "responseTransformer was null");
        mResponseTransformer = responseTransformer;
    }

    public void setMessageTransformer(final TargetedMessageTransformer messageTransformer) {
        notNull(messageTransformer, "messageTransformer was null");
        mMessageTransformer = messageTransformer;
    }

    public void setSocketReadTimeout(int socketReadTimeout) {
        Validate.isTrue(socketReadTimeout > 0);
        mSocketReadTimeout = socketReadTimeout;
    }
}
