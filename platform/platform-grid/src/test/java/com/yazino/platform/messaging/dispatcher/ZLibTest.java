package com.yazino.platform.messaging.dispatcher;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;

public class ZLibTest {

    private static final String PLAIN_TEXT = "aSampleStringForCompression";
    private static final String COMPRESSED_TEXT = "eJxLDE7MLchJDS4pysxLd8svcs7PLShKLS7OzM8DAJZ9CvQ=";

    @Test
    public void inputCanBeDeflated() throws IOException {
        final String output = ZLib.deflate(PLAIN_TEXT);

        MatcherAssert.assertThat(output, Matchers.is(Matchers.equalTo(COMPRESSED_TEXT)));
    }

    @Test
    public void base64CompressedInputCanBeInflated() throws IOException {
        final String output = ZLib.inflate(COMPRESSED_TEXT);

        MatcherAssert.assertThat(output, Matchers.is(Matchers.equalTo(PLAIN_TEXT)));
    }

    @Test
    public void aDeflatedItemCanBeInflatedAgain() throws IOException {
        final String compressed = ZLib.deflate(PLAIN_TEXT);

        MatcherAssert.assertThat(ZLib.inflate(compressed), Matchers.is(Matchers.equalTo(PLAIN_TEXT)));
    }

    @Test
    public void whenNullIsInflatedANullIsReturned() throws IOException {
        MatcherAssert.assertThat(ZLib.inflate(null), Matchers.is(Matchers.nullValue()));
    }

    @Test
    public void whenNullIsDeflatedANullIsReturned() throws IOException {
        MatcherAssert.assertThat(ZLib.deflate(null), Matchers.is(Matchers.nullValue()));
    }

    @Test(expected = IOException.class)
    public void inflatingAnInvalidInputStringCausesAnIOException() throws IOException {
        ZLib.inflate("anInvalidString");
    }
}
