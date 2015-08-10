package strata.server.performance.rabbit;

import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests the {@link QuietCloser} class.
 */
public class QuietCloserTest {

    @Test
    public void shouldCloseACloseableObject() throws Exception {
        Closeable closeable = mock(Closeable.class);
        QuietCloser.closeQuietly(closeable);
        verify(closeable).close();
    }

    @Test
    public void shouldSwallowACloseablesIOException() throws Exception {
        Closeable closeable = mock(Closeable.class);
        doThrow(new IOException()).when(closeable).close();
        QuietCloser.closeQuietly(closeable);
    }

    @Test
    public void shouldExecuteACloseMethodOnANonCloseableObject() {
        class ObjectWithCloseMethod {
            boolean mClosed = false;

            public void close() {
                mClosed = true;
            }

            public boolean isClosed() {
                return mClosed;
            }
        }
        ObjectWithCloseMethod withCloseMethod = new ObjectWithCloseMethod();
        QuietCloser.closeQuietly(withCloseMethod);
        assertTrue(withCloseMethod.isClosed());
    }

    @Test
    public void shouldSwallowExceptionWhenANonCloseableObjectThrows() {
        class ObjectWithCloseMethod {
            boolean mCalled = false;

            public void close() {
                mCalled = true;
                throw new UnsupportedOperationException("close not supported");
            }

            public boolean wasCalled() {
                return mCalled;
            }

        }
        ObjectWithCloseMethod withCloseMethod = new ObjectWithCloseMethod();
        QuietCloser.closeQuietly(withCloseMethod);
        assertTrue(withCloseMethod.wasCalled());
    }

    @Test
    public void shouldSwallowExceptionWhenNoCloseMethodExists() {
        QuietCloser.closeQuietly(new Object());
    }
}
