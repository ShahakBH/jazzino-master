package strata.server.performance.rabbit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests the {@link ReplayableFileSource} class.
 */
public class ReplayableFileSourceTest {

    private ReplayableFileSource source;
    private File file;

    @Before
    public void setup() throws Exception {
        String contents = "test-exchange,1000,json,java.lang.Object\n" +
                "PLAYER.1234,{\"a\"=123,\"b\"=999}\n" +
                "PLAYER.5678,{\"c\"=455,\"d\"=1}";
        file = writeFile(contents);
        source = new ReplayableFileSource(file.getPath());
        source.init();
    }

    @Test
    public void shouldReadExchangeFromFirstLine() {
        assertEquals("test-exchange", source.getExchange());
    }

    @Test
    public void shouldReadMessageIntervalFromFirstLine() {
        assertEquals(1000, source.getMessageInterval());
    }

    @Test
    public void shouldReturnMessageWithContentFromSecondLineWhenNextCalled() throws Exception {
        assertTrue(source.hasNext());
        ReplayableMessage message = source.next();
        assertEquals("PLAYER.1234", message.getRoutingKey());
        assertEquals("{\"a\"=123,\"b\"=999}", new String(message.getBody()));
    }

    @Test
    public void shouldReturnMessageWithContentFromThirdLineWhenNextCalledTwice() throws Exception {
        assertTrue(source.hasNext());
        source.next();
        assertTrue(source.hasNext());
        ReplayableMessage message = source.next();
        assertEquals("PLAYER.5678", message.getRoutingKey());
        assertEquals("{\"c\"=455,\"d\"=1}", new String(message.getBody()));
    }

    @Test
    public void shouldReturnFalseWhenNoMoreLinesToRead() {
        assertTrue(source.hasNext());
        source.next();
        assertTrue(source.hasNext());
        source.next();
        assertFalse(source.hasNext());
    }

    @Test
    public void shouldReturnSecondLineWhenResetCalledAndNextInvoked() {
        ReplayableMessage first = source.next();
        ReplayableMessage second = source.next();
        assertFalse(first.equals(second));
        source.reset();
        ReplayableMessage next = source.next();
        assertEquals(first, next);
    }

    @After
    public void tearDown() {
        source.destroy();
        file.delete();
    }

    private static File writeFile(String contents) throws IOException {
        File file = File.createTempFile(ReplayableFileSourceTest.class.getSimpleName(), "txt");
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(contents.getBytes("UTF-8"));
        }
        finally {
            QuietCloser.closeQuietly(outputStream);
        }
        return file;
    }

}
