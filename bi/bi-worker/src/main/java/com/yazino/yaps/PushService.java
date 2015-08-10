package com.yazino.yaps;

import org.apache.commons.lang3.Validate;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Provides access to Apple's push service. Note that one of these is expected per game.
 */
public class PushService implements DisposableBean {
    private static final Logger LOG = LoggerFactory.getLogger(PushService.class);

    private final GenericObjectPool<AppleConnection> mConnectionPool;
    private MessagePusher mMessagePusher = new MessagePusher();

    public PushService(GenericObjectPool<AppleConnection> pool) {
        notNull(pool);
        mConnectionPool = pool;
    }

    /**
     * Push the specified message to apple.
     *
     * @param message not null or empty
     * @return the response from the server
     * @throws Exception should any exception occur, in general only {@link RecoverableException}s should cause a message retry.
     */
    public PushResponse pushMessage(final TargetedMessage message) throws Exception {
        PushResponse response = PushResponse.OK;

        AppleConnection connection = mConnectionPool.borrowObject();

        try {
            response = mMessagePusher.pushAndReadResponse(connection, message);

            if (response != PushResponse.OK) {
                // apple shuts down the socket if the message sent to them is malformed and a response is sent
                LOG.debug("Manually shutting socket as response was [{}] for message [{}]", response, message);
                QuietCloser.closeQuietly(connection);
            }
        } catch (IOException e) {
            QuietCloser.closeQuietly(connection);
            throw new RecoverableException(e);
        } finally {
            mConnectionPool.returnObject(connection);
        }
        return response;
    }

    @Override
    public void destroy() throws Exception {
        QuietCloser.closeQuietly(mConnectionPool);
    }

    public void setMessagePusher(MessagePusher messagePusher) {
        Validate.notNull(messagePusher);
        mMessagePusher = messagePusher;
    }
}
