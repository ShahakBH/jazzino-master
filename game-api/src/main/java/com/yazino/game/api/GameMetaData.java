package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Extended meta-data for game types.
 * <p/>
 * Extended meta-data is information about strings, resources and other such
 * items. Information about identity and feature support should live in
 * {@link GameType}.
 * <p/>
 * You can build these using {@link GameMetaDataBuilder}.
 */
public class GameMetaData implements Serializable {
    private static final long serialVersionUID = -8627661570294061701L;

    private final Map<GameMetaDataKey, Object> data = new HashMap<GameMetaDataKey, Object>();

    GameMetaData(final Map<GameMetaDataKey, Object> data) {
        if (data != null) {
            this.data.putAll(data);
        }
    }

    /**
     * Fetch a string value for a given key.
     * 
     * @param key the key to retrieve.
     * @return the string value of the key, or null if not present.
     */
    public String forKey(final GameMetaDataKey key) {
        final Object value = data.get(key);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final GameMetaData rhs = (GameMetaData) obj;
        return new EqualsBuilder()
                .append(data, rhs.data)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(data)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(data)
                .toString();
    }
}
