package com.yazino.game.api;

import com.yazino.game.api.document.Documentable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * GameStatus encapsulates the game state, and is stored while processing is not occurring. As such,
 * this must be a data only class, and must be deserializable on a clean JVM - that is, it cannot depend
 * on game classes.
 * <p/>
 * The data within must be either primitive, string, date or a nested collection of the same.
 */
public class GameStatus implements Serializable, Map<String, Object> {
    private static final long serialVersionUID = -2569786674930421625L;

    private final Map<String, Object> document;

    /**
     * Create a new status.
     *
     * @param document the documentable for the status. May not be null.
     */
    public GameStatus(final Map<String, Object> document) {
        notNull(document, "documentable may not be null");

        this.document = document;
    }

    /**
     * Create a new status.
     *
     * @param documentable the documentable object for the status. May not be null.
     */
    public GameStatus(final Documentable documentable) {
        notNull(documentable, "documentable may not be null");

        this.document = documentable.toDocument();
    }

    /**
     * Get the read-only status data for the game.
     *
     * @return the status data as a map of string to primitives, strings, dates or nested collections of the same.
     */
    public Map<String, Object> getDocument() {
        return document;
    }

    @Override
    public int size() {
        return document.size();
    }

    @Override
    public boolean isEmpty() {
        return document.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return document.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return document.containsValue(value);
    }

    @Override
    public Object get(final Object key) {
        return document.get(key);
    }

    @Override
    public Object put(final String key, final Object value) {
        return document.put(key, value);
    }

    @Override
    public Object remove(final Object key) {
        return document.remove(key);
    }

    @Override
    public void putAll(final Map<? extends String, ?> map) {
        document.putAll(map);
    }

    @Override
    public void clear() {
        document.clear();
    }

    @Override
    public Set<String> keySet() {
        return document.keySet();
    }

    @Override
    public Collection<Object> values() {
        return document.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return document.entrySet();
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
        final GameStatus rhs = (GameStatus) obj;
        return new EqualsBuilder()
                .append(document, rhs.document)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(document)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(document)
                .toString();
    }
}
