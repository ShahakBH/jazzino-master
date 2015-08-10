package com.yazino.platform.messaging.dispatcher;

import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import static org.apache.commons.codec.binary.Base64.encodeBase64;

public final class ZLib {

    private ZLib() {
        // utility class
    }

    public static String inflate(final String base64EncodedZlibData) throws IOException {
        if (base64EncodedZlibData == null) {
            return null;
        }

        final byte[] compressed = Base64.decodeBase64(base64EncodedZlibData.getBytes(Charsets.UTF_8));
        final ByteArrayOutputStream inflated = new ByteArrayOutputStream();
        final InflaterOutputStream inflatedOs = new InflaterOutputStream(inflated);
        inflatedOs.write(compressed);

        inflatedOs.close();

        return inflated.toString("UTF-8");
    }


    public static String deflate(final String input) throws IOException {
        if (input == null) {
            return null;
        }

        final ByteArrayOutputStream deflated = new ByteArrayOutputStream();

        DeflaterOutputStream deflaterOs = null;
        Writer deflaterWriter = null;
        try {
            deflaterOs = new DeflaterOutputStream(deflated);
            deflaterWriter = new OutputStreamWriter(deflaterOs, Charsets.UTF_8);

            deflaterWriter.write(input);

        } finally {
            closeQuietly(deflaterWriter, deflaterOs);
        }

        return new String(encodeBase64(deflated.toByteArray()), "UTF-8");
    }

    private static void closeQuietly(final Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
    }
}
