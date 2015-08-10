package com.yazino.platform.lightstreamer.adapter;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * Provides common methods for inflating / deflating compressed data.
 */
public final class CompressionUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CompressionUtils.class);

    public static final String UTF_8 = "UTF-8";

    private CompressionUtils() {
    }

    public static byte[] fromBase64(final byte[] data) {
        if (data == null) {
            return null;
        }
        return Base64.decodeBase64(data);
    }

    public static String inflate(final byte[] base64EncodedZlibData) throws IOException {
        byte[] compressed = fromBase64(base64EncodedZlibData);
        ByteArrayOutputStream inflated = new ByteArrayOutputStream();
        InflaterOutputStream inflatedOs = new InflaterOutputStream(inflated);

        try {
            inflatedOs.write(compressed);
        } finally {
            closeQuietly(inflatedOs);
        }

        return inflated.toString(UTF_8);
    }

    public static String toBase64(final byte[] data) throws UnsupportedEncodingException {
        if (data == null) {
            return null;
        }
        return new String(Base64.encodeBase64(data), UTF_8);
    }

    public static byte[] deflate(final String input) throws IOException {
        if (input == null) {
            return null;
        }

        ByteArrayOutputStream deflated = new ByteArrayOutputStream();
        DeflaterOutputStream deflaterOs = new DeflaterOutputStream(deflated);
        Writer deflaterWriter = new OutputStreamWriter(deflaterOs);

        try {
            deflaterWriter.write(input);
        } finally {
            closeQuietly(deflaterWriter);
            closeQuietly(deflaterOs);
        }

        return deflated.toByteArray();
    }

    private static void closeQuietly(Closeable stream) {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (Exception e) {
            LOG.debug("Attempt to close {} caused {}", stream, e.getMessage());
        }
    }
}
