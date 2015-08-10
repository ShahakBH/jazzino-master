package com.yazino.game.api;

import java.util.HashMap;
import java.util.Map;

/**
 * A build for {@link GameMetaData}.
 */
public class GameMetaDataBuilder {

    private final Map<GameMetaDataKey, Object> data = new HashMap<GameMetaDataKey, Object>();

    public GameMetaDataBuilder with(final GameMetaDataKey key, final Object value) {
        if (key == null) {
            throw new IllegalArgumentException("key may not be null");
        }

        this.data.put(key, value);
        return this;
    }

    /**
     * Build a meta data object.
     *
     * @return the new game meta data. Never null.
     */
    public GameMetaData build() {
        return new GameMetaData(data);
    }
}
