package com.yazino.yaps;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wraps a socket and its close future object.
 */
public class AppleConnection implements Closeable {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final String mID;
    private final Socket mSocket;
    private ScheduledFuture mCloseFuture;

    public AppleConnection(Socket socket) {
        Validate.notNull(socket);
        mSocket = socket;
        mID = String.format("#%d/%s:%d", COUNTER.incrementAndGet(), socket.getInetAddress().toString(), socket.getPort());
    }

    // test purposes only
    AppleConnection(String id, Socket socket) {
        Validate.notNull(id);
        Validate.notNull(socket);
        mSocket = socket;
        mID = id;
    }

    public boolean isOpen() {
        return !mSocket.isClosed() && mSocket.isConnected();
    }

    @Override
    public void close() throws IOException {
        if (mCloseFuture != null) {
            mCloseFuture.cancel(false);
        }
        QuietCloser.closeQuietly(mSocket);
    }

    public Socket getSocket() {
        return mSocket;
    }

    public String getConnectionID() {
        return mID;
    }

    public ScheduledFuture getCloseFuture() {
        return mCloseFuture;
    }

    public void setCloseFuture(ScheduledFuture closeFuture) {
        mCloseFuture = closeFuture;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("mID", mID).
                toString();
    }
}
