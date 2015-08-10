package com.yazino.bi.operations.util;

import java.io.BufferedWriter;
import java.io.Writer;

public interface WriterFactory {

    BufferedWriter getWriter(Writer outputStream);

}
