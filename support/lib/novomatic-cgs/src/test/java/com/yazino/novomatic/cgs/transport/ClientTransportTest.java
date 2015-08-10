package com.yazino.novomatic.cgs.transport;

import org.apache.commons.pool.ObjectPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.ByteBuffer;

import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClientTransportTest {

    private static final byte[] REQUEST = new byte[]{1, 2, 3};
    private static final byte[] REQUEST_LENGTH = ByteBuffer.allocate(4).putInt(REQUEST.length).array();
    private static final byte[] RESPONSE = new byte[]{5, 5};
    private static final byte[] RESPONSE_LENGTH = ByteBuffer.allocate(4).putInt(RESPONSE.length).array();

    @Mock
    private ObjectPool<ClientSocketConnection> pool;
    @Mock
    private ClientSocketConnection connection;

    private ClientTransport underTest;

    @Before
    public void setUp() {
        underTest = new ClientTransport(pool);
    }

    @Test
    public void shouldExecuteRequest() throws Exception {
        when(pool.borrowObject()).thenReturn(connection);
        when(connection.receive(4)).thenReturn(RESPONSE_LENGTH);
        when(connection.receive(RESPONSE.length)).thenReturn(RESPONSE);

        final byte[] actualResponse = underTest.sendRequest(REQUEST);

        assertThat(actualResponse, equalTo(RESPONSE));
        verify(pool).returnObject(connection);
    }

    @Test
    public void shouldThrowIOException() throws Exception {
        when(pool.borrowObject()).thenReturn(connection);
        doThrow(new IOException("bang")).when(connection).send(REQUEST);

        try {
            underTest.sendRequest(REQUEST);
            fail("Exception not thrown");
        } catch (Exception e) {

        } finally {
            verify(pool, times(1)).invalidateObject(connection);
            verify(pool, times(0)).returnObject(connection);
        }


    }

}
