package com.yazino.yaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A connection to apple that will attempt to keep the socket open for a specified amount of time.
 * In reality, this does nothing to keep the socket alive, it simple doesn't close it.
 * It does however schedule a close task to close the socket after a certain amount of idle time, which gets reset
 * everytime the socket is used.
 */
public class KeepAliveAppleConnection implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(KeepAliveAppleConnection.class);

    private final Object lock = new Object();
    private final Map<String, OpenedSocket> reusableSockets = new HashMap<String, OpenedSocket>();
    private final AppleSocketFactory socketFactory;

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
    private long socketTimeout = TimeUnit.MINUTES.toMillis(2);

    public KeepAliveAppleConnection(final AppleSocketFactory socketFactory) {
        notNull(socketFactory, "socketFactory was null");
        this.socketFactory = socketFactory;
    }

    /**
     * Returns an opened socket.
     * Users should in general not close the returned socket themselves, instead use this objects closeSocket method.
     *
     * @param gameType not null or empty
     * @return a socket to the underlying's connection host / port
     * @throws IOException              should opening the socket fail
     * @throws UnsupportedGameException should the gameType not be supported, in general because it has no config
     */
    public Socket openSocket(final String gameType) throws IOException, UnsupportedGameException {
        OpenedSocket openedSocket = null;
        synchronized (lock) {
            if (reusableSockets.containsKey(gameType)) {
                openedSocket = reusableSockets.get(gameType);
                final Socket socket = openedSocket.getSocket();
                if (socket.isClosed() || !socket.isConnected()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format(
                                "Socket for gameType [%s] has been closed externally so cancelling close future",
                                gameType));
                    }
                    cancelClose(openedSocket);
                    QuietCloser.closeQuietly(socket);
                    openedSocket = null;
                }
            }
            if (openedSocket == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Creating new socket for gameType [%s]", gameType));
                }

                openedSocket = new OpenedSocket(socketFactory.newSocket());
                reusableSockets.put(gameType, openedSocket);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Reusing socket for [%s]", gameType));
                }
            }
            rescheduleClose(openedSocket);
        }
        return openedSocket.getSocket();
    }

    private void rescheduleClose(final OpenedSocket openedSocket) {
        cancelClose(openedSocket);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Scheduling closer to run in [%d]ms", socketTimeout));
        }

        final ScheduledFuture closeFuture = executorService.schedule(
                new ClosingRunnable(openedSocket.getSocket()), socketTimeout, TimeUnit.MILLISECONDS);
        openedSocket.setCloseFuture(closeFuture);
    }

    private void cancelClose(final OpenedSocket openedSocket) {
        final ScheduledFuture closeFuture = openedSocket.getCloseFuture();
        if (closeFuture != null) {
            final boolean cancelled = closeFuture.cancel(false);
            if (LOG.isDebugEnabled()) {
                final String cancelledText;
                if (cancelled) {
                    cancelledText = "";
                } else {
                    cancelledText = "not";
                }
                LOG.debug(String.format("Close future was %s closed", cancelledText));
            }
            openedSocket.setCloseFuture(null);
        }
    }

    /**
     * Ignores any requests to close the socket.
     *
     * @param socket not null or empty
     */
    public void closeSocket(final Socket socket) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Ignoring call to closeSocket [%s], a separate thread"
                    + " will close the socket after the lapsed timeout", socket));
        }
    }

    @Override
    public void close() throws IOException {
        executorService.shutdownNow();
        for (OpenedSocket openedSocket : reusableSockets.values()) {
            QuietCloser.closeQuietly(openedSocket.getSocket());
        }
    }

    final void setExecutorService(final ScheduledExecutorService executorService) {
        notNull(executorService, "executorService was null");
        this.executorService = executorService;
    }

    public void setSocketTimeout(final long socketTimeout) {
        if (socketTimeout < 1) {
            throw new IllegalArgumentException("timeout was invalid (<1)");
        }
        this.socketTimeout = socketTimeout;
    }

    public long getSocketTimeout() {
        return socketTimeout;
    }

    final Map<String, OpenedSocket> getReusableSockets() {
        return Collections.unmodifiableMap(reusableSockets);
    }

    static final class OpenedSocket {

        private ScheduledFuture closeFuture;
        private final Socket socket;

        private OpenedSocket(final Socket socket) {
            this.socket = socket;
        }

        public Socket getSocket() {
            return socket;
        }

        public ScheduledFuture getCloseFuture() {
            return closeFuture;
        }

        public void setCloseFuture(final ScheduledFuture closeFuture) {
            this.closeFuture = closeFuture;
        }
    }
}
