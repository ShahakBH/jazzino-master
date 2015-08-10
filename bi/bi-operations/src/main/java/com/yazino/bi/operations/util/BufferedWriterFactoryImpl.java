package com.yazino.bi.operations.util;

import java.io.BufferedWriter;
import java.io.Writer;

public class BufferedWriterFactoryImpl implements WriterFactory {
    @Override
    public BufferedWriter getWriter(final Writer outputStream) {
        return new BufferedWriter(outputStream);
    }

}
