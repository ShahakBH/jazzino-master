package com.yazino.yaps;

import org.apache.commons.lang3.Validate;
import org.apache.commons.pool.PoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link PoolableObjectFactory} that is responsible for creating objects to be used in an
 * {@link org.apache.commons.pool.ObjectPool}.
 */
public class AppleConnectionFactory implements PoolableObjectFactory<AppleConnection> {

    private static final Logger LOG = LoggerFactory.getLogger(AppleConnectionFactory.class);

    private final AppleSocketFactory mSocketFactory;
    private ScheduledExecutorService mExecutorService = Executors.newScheduledThreadPool(20);
    private long mSocketTTL = TimeUnit.MINUTES.toMillis(2);

    public AppleConnectionFactory(AppleSocketFactory socketFactory) {
        Validate.notNull(socketFactory);
        mSocketFactory = socketFactory;
    }

    @Override
    public AppleConnection makeObject() throws Exception {
        Socket socket = mSocketFactory.newSocket();
        AppleConnection connection = new AppleConnection(socket);
        LOG.debug("[{}] created.", connection);
        return connection;
    }

    @Override
    public void destroyObject(AppleConnection connection) throws Exception {
        QuietCloser.closeQuietly(connection);
    }

    @Override
    public boolean validateObject(AppleConnection connection) {
        boolean open = connection.isOpen();
        LOG.debug("validateObject: [{}] is {}", connection, open);
        return open;
    }

    @Override
    public void activateObject(AppleConnection connection) throws Exception {
        ScheduledFuture closeFuture = connection.getCloseFuture();
        if (closeFuture == null) {
            LOG.debug("activateObject: [{}] had a null close future", connection);
        } else {
            LOG.debug("activateObject: Cancelling [{}] close future", connection);
            closeFuture.cancel(false);
        }
    }

    @Override
    public void passivateObject(AppleConnection connection) throws Exception {
        if (connection.getCloseFuture() != null) {
            connection.getCloseFuture().cancel(false);
        }
        ScheduledFuture<?> future = mExecutorService.schedule(new ClosingRunnable(connection), mSocketTTL, TimeUnit.MILLISECONDS);
        connection.setCloseFuture(future);
        LOG.debug("passivateObject: Scheduled [{}] to close in [{}]ms", connection, mSocketTTL);
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return mExecutorService;
    }

    public void setScheduledExecutorService(ScheduledExecutorService executorService) {
        Validate.notNull(executorService);
        mExecutorService = executorService;
    }

    public long getSocketTTL() {
        return mSocketTTL;
    }

    public void setSocketTTL(long socketTTL) {
        Validate.isTrue(socketTTL > 0);
        mSocketTTL = socketTTL;
    }

}


