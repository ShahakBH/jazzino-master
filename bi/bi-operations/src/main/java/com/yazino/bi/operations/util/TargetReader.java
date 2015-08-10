package com.yazino.bi.operations.util;

import com.yazino.bi.operations.engagement.AppRequestTarget;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Reads an input stream and returns the player IDs matching it
 */
public interface TargetReader {
    /**
     * Reads the IDs stream
     *
     * @param stream Input stream to read
     * @return IosPushNotificationTarget containing collection of Game type and playerID combinations
     * @throws java.io.IOException Thrown if the stream unexpectedly fails
     */
    Set<AppRequestTarget> readTargets(InputStream stream) throws IOException;
}
