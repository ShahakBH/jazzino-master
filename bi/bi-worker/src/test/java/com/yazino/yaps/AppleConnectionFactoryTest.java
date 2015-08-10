package com.yazino.yaps;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AppleConnectionFactoryTest {

    private final ScheduledExecutorService mExecutorService = mock(ScheduledExecutorService.class);
    private final Socket mSocket = mock(Socket.class);
    private final AppleSocketFactory mSocketFactory = mock(AppleSocketFactory.class);
    private final AppleConnectionFactory mConnectionFactory = new AppleConnectionFactory(mSocketFactory);

    @Before
    public void setup() throws Exception {
        when(mSocket.getInetAddress()).thenReturn(InetAddress.getByName("localhost"));
        when(mSocketFactory.newSocket()).thenReturn(mSocket);
        mConnectionFactory.setScheduledExecutorService(mExecutorService);
    }

    @Test
    public void shouldUseSocketFactoryToMakeObject() throws Exception {
        AppleConnection connection = mConnectionFactory.makeObject();
        assertEquals(mSocket, connection.getSocket());
        verify(mSocketFactory).newSocket();
    }

    @Test
    public void shouldCloseConnectionWhenDestroyingIt() throws Exception {
        AppleConnection connection = mock(AppleConnection.class);
        mConnectionFactory.destroyObject(connection);
        verify(connection).close();
    }

    @Test
    public void shouldCancelConnectionsCloseFutureWhenDestroyingIt() throws Exception {
        ScheduledFuture future = mock(ScheduledFuture.class);
        AppleConnection connection = new AppleConnection(mSocket);
        connection.setCloseFuture(future);
        mConnectionFactory.destroyObject(connection);
        verify(future).cancel(false);
    }

    @Test
    public void shouldFailConnectionValidationWhenSocketIsClosed() throws Exception {
        assertConnectionValidation(true, true, false);
    }

    @Test
    public void shouldFailConnectionValidationWhenSocketIsDisconnected() throws Exception {
        assertConnectionValidation(false, false, false);
    }

    @Test
    public void shouldFailConnectionValidationWhenSocketIsClosedAndDisconnected() throws Exception {
        assertConnectionValidation(true, false, false);
    }

    @Test
    public void shouldPassConnectionValidationWhenSocketIsOpenAndConnected() throws Exception {
        assertConnectionValidation(false, true, true);
    }

    private void assertConnectionValidation(boolean closed, boolean connected, boolean expectedValidity) {
        when(mSocket.isClosed()).thenReturn(closed);
        when(mSocket.isConnected()).thenReturn(connected);
        AppleConnection connection = new AppleConnection(mSocket);
        boolean actualValidity = mConnectionFactory.validateObject(connection);
        assertEquals(expectedValidity, actualValidity);
    }

    @Test
    public void shouldScheduleCloseWhenPassivatingConnection() throws Exception {
        ScheduledFuture future = mock(ScheduledFuture.class);
        when(mExecutorService.schedule(any(ClosingRunnable.class), anyLong(), any(TimeUnit.class))).thenReturn(future);

        AppleConnection connection = new AppleConnection(mSocket);
        mConnectionFactory.setSocketTTL(500);
        assertEquals(500, mConnectionFactory.getSocketTTL());
        mConnectionFactory.passivateObject(connection);

        ArgumentCaptor<ClosingRunnable> argument = ArgumentCaptor.forClass(ClosingRunnable.class);

        verify(mExecutorService).schedule(argument.capture(), eq(mConnectionFactory.getSocketTTL()), eq(TimeUnit.MILLISECONDS));
        assertEquals(connection, argument.getValue().getToClose());
        assertEquals(future, connection.getCloseFuture());
    }

    @Test
    public void shouldCancelAlreadyExistingCloseFutureWhenPassivatingConnection() throws Exception {
        ScheduledFuture future = mock(ScheduledFuture.class);
        AppleConnection connection = new AppleConnection(mSocket);
        connection.setCloseFuture(future);
        mConnectionFactory.passivateObject(connection);
        verify(future).cancel(false);
    }

    @Test
    public void shouldHandleWhenConnectionsCloseFutureIsNullDuringActivation() throws Exception {
        AppleConnection connection = new AppleConnection(mSocket);
        connection.setCloseFuture(null);
        mConnectionFactory.activateObject(connection);
    }

    @Test
    public void shouldCancelConnectionsCloseFutureWhenActivatingConnection() throws Exception {
        AppleConnection connection = new AppleConnection(mSocket);
        ScheduledFuture future = mock(ScheduledFuture.class);
        connection.setCloseFuture(future);
        mConnectionFactory.activateObject(connection);
        verify(future).cancel(false);
    }

}
