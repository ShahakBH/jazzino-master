package com.yazino.yaps;

import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class QuietCloserTest {

    @Test
    public void shouldHandleNullObjectWithoutThrowingException() {
        QuietCloser.closeQuietly(null);
    }

    @Test
    public void shouldHandleObjectWithoutCloseMethod() {
        QuietCloser.closeQuietly(new Object());
    }

    @Test
    public void shouldHandleObjectWithCloseMethodWithParameters() {
        QuietCloser.closeQuietly(new ObjectWithCloseWithParameters());
    }

    @Test
    public void shouldCloseObject() {
        ObjectWithClose toClose = new ObjectWithClose();
        assertFalse(toClose.closed);
        QuietCloser.closeQuietly(toClose);
        assertTrue(toClose.closed);
    }

    @Test
    public void shouldCatchCloseMethodsThrownException() throws Exception {
        Closeable closeable = mock(Closeable.class);
        doThrow(new IOException("mock exception")).when(closeable).close();
        QuietCloser.closeQuietly(closeable);
    }

    private static class ObjectWithClose {

        boolean closed = false;

        public void close() {
            closed = true;
        }
    }

    private static class ObjectWithCloseWithParameters {
        public void close(Object foo) {
            throw new RuntimeException("should not have been called");
        }
    }
}
