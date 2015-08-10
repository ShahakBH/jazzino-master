package com.yazino.bi.operations.util;

import com.yazino.bi.operations.model.PlayerReaderResult;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads an input stream and returns the player IDs matching it
 */
public interface PlayerIdReader {
    /**
     * Reads the IDs stream
     *
     * @param stream Input stream to read
     * @return PlayerReaderResult containing collection of mapped players, list of unmatched external ids and a
     *         list of external ids mapped to mutliple player ids
     * @throws IOException Thrown if the stream unexpectedly fails
     */
    PlayerReaderResult readPlayerIds(InputStream stream) throws IOException;
}
