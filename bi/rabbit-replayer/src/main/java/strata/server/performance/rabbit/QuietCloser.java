package strata.server.performance.rabbit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.Method;

/**
 * Provides ability to close any object with a close method, whilst swallowing any
 * exceptions.
 */
public final class QuietCloser {
    private static final Logger LOG = LoggerFactory.getLogger(QuietCloser.class);

    private QuietCloser() {
        // utility class
    }

    public static void closeQuietly(final Object object) {
        if (object == null) {
            return;
        }
        try {
            if (object instanceof Closeable) {
                ((Closeable) object).close();
            } else {
                invokeCloseMethod(object);
            }
        } catch (Throwable e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Closing [%s] produced a [%s]", object, e.getClass().getSimpleName()));
            }
        }
    }

    private static void invokeCloseMethod(final Object object) throws Exception {
        final Method method = object.getClass().getMethod("close");
        method.invoke(object);
    }
}
