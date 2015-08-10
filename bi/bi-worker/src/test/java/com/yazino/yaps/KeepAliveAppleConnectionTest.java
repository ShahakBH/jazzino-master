package com.yazino.yaps;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class KeepAliveAppleConnectionTest {

    private final String gameType = "TEST";

    private final ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
    private final AppleSocketFactory underlyingSocketFactory = mock(AppleSocketFactory.class);
    private final Socket socket = mock(Socket.class);
    private final KeepAliveAppleConnection keepAliveConnection = new KeepAliveAppleConnection(underlyingSocketFactory);

    @Before
    public void setup() {
        keepAliveConnection.setExecutorService(executorService);
        when(socket.isConnected()).thenReturn(true);
    }

    @Test
    public void shouldOpenNewSocketIfOneDoesNotExistForGame() throws Exception {
        when(underlyingSocketFactory.newSocket()).thenReturn(socket);
        Socket opened = keepAliveConnection.openSocket(gameType);
        assertEquals(socket, opened);
        verify(underlyingSocketFactory).newSocket();
    }

    @Test
    public void shouldStoreSocketAfterCreated() throws Exception {
        when(underlyingSocketFactory.newSocket()).thenReturn(socket);
        assertEquals(socket, keepAliveConnection.openSocket(gameType));
        assertTrue(keepAliveConnection.getReusableSockets().containsKey(gameType));
        assertEquals(socket, keepAliveConnection.getReusableSockets().get(gameType).getSocket());
    }

    @Test
    public void shouldReuseSocketIfAnOpenOneExistsForGame() throws Exception {
        when(underlyingSocketFactory.newSocket()).thenReturn(socket);
        assertEquals(socket, keepAliveConnection.openSocket(gameType));
        assertEquals(socket, keepAliveConnection.openSocket(gameType));
        verify(underlyingSocketFactory, times(1)).newSocket();
    }

    @Test
    public void shouldOpenNewSocketIfExistingOneIsClosed() throws Exception {
        Socket newSocket = mock(Socket.class);
        when(underlyingSocketFactory.newSocket()).thenReturn(socket).thenReturn(newSocket);
        assertEquals(socket, keepAliveConnection.openSocket(gameType));
        when(socket.isClosed()).thenReturn(true);
        assertEquals(newSocket, keepAliveConnection.openSocket(gameType));
        verify(underlyingSocketFactory, times(2)).newSocket();
    }

    @Test
    public void shouldScheduleCloserWhenCreatingNewSocket() throws Exception {
        long closeDelay = 500L;
        keepAliveConnection.setSocketTimeout(closeDelay);
        when(underlyingSocketFactory.newSocket()).thenReturn(socket);
        assertEquals(socket, keepAliveConnection.openSocket(gameType));
        verify(executorService).schedule(any(ClosingRunnable.class), eq(closeDelay), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldRescheduleCloserWhenReusingExistingSocket() throws Exception {
        ScheduledFuture future = mock(ScheduledFuture.class);
        when(executorService.schedule(any(ClosingRunnable.class), eq(keepAliveConnection.getSocketTimeout()), eq(TimeUnit.MILLISECONDS))).thenReturn(future);
        when(underlyingSocketFactory.newSocket()).thenReturn(socket);
        assertEquals(socket, keepAliveConnection.openSocket(gameType));
        assertEquals(socket, keepAliveConnection.openSocket(gameType));
        verify(executorService, times(2)).schedule(any(ClosingRunnable.class), eq(keepAliveConnection.getSocketTimeout()), eq(TimeUnit.MILLISECONDS));
        verify(future, times(1)).cancel(false);
    }

    @Test
    public void shouldNotInteractWithSocketWhenCloseSocketCalled() {
        keepAliveConnection.closeSocket(socket);
        verifyZeroInteractions(socket);
    }

    @Test
    public void shouldShutdownExecutorServiceWhenClosed() throws Exception {
        keepAliveConnection.close();
        verify(executorService).shutdownNow();
    }

    @Test
    public void shouldCloseAllSocketsWhenClosed() throws Exception {
        when(underlyingSocketFactory.newSocket()).thenReturn(socket);
        keepAliveConnection.openSocket("TESTA");
        keepAliveConnection.openSocket("TESTB");
        keepAliveConnection.openSocket("TESTC");
        keepAliveConnection.close();
        verify(socket, times(3)).close();
    }

    @Test(expected = IOException.class)
    public void shouldThrowExceptionIfSocketCannotBeOpened() throws Exception {
        when(underlyingSocketFactory.newSocket()).thenThrow(new IOException());
        keepAliveConnection.openSocket(gameType);
    }

}
