package com.yazino.yaps;

import org.junit.Test;

import java.io.IOException;
import java.net.Socket;

import static org.mockito.Mockito.*;

public class ClosingRunnableTest {

    private final Socket socket = mock(Socket.class);
    private final ClosingRunnable closer = new ClosingRunnable(socket);

    @Test(expected = NullPointerException.class)
    public void shouldNotBeConstructableWithNullSocket() {
        new ClosingRunnable(null);
    }

    @Test
    public void shouldCloseSocket() throws Exception {
        closer.run();
        verify(socket).close();
    }

    @Test
    public void shouldNotRethrowAnyExceptionsWhenClosingSocket() throws Exception {
        doThrow(new IOException()).when(socket).close();
        closer.run();
        verify(socket).close();
    }

}
