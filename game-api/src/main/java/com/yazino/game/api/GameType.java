package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;


/**
 * Core meta-data for a given type of game.
 * <p/>
 * Core meta-data is information related to a game's identity and
 * to its capabilities.
 * <p/>
 * Extended meta-data should be stored in {@link GameMetaData}.
 */
public class GameType implements Serializable {
    private static final long serialVersionUID = 1694541259960062149L;

    private final Set<String> pseudonyms = new HashSet<String>();
    private final Set<GameFeature> features = new HashSet<GameFeature>();

    private final String id;
    private final String name;

    /**
     * Create a game type with no optional features.
     *
     * @param id         the unique string ID of the game type.
     * @param name       the presentable name of the game type.
     * @param pseudonyms any suitable pseudonyms of the game type, e.g. as a user might enter. May be null.
     */
    public GameType(final String id,
                    final String name,
                    final Set<String> pseudonyms) {
        this(id, name, pseudonyms, null);
    }

    /**
     * Create a game type.
     *
     * @param id         the unique string ID of the game type.
     * @param name       the presentable name of the game type.
     * @param pseudonyms any suitable pseudonyms of the game type, e.g. as a user might enter. May be null.
     * @param features   optional features this game supports.
     */
    public GameType(final String id,
                    final String name,
                    final Set<String> pseudonyms,
                    final Set<GameFeature> features) {
        notNull(id, "id may not be null");
        notNull(name, "name may not be null");

        this.id = id;
        this.name = name;

        if (pseudonyms != null) {
            this.pseudonyms.addAll(pseudonyms);
        }
        if (features != null) {
            this.features.addAll(features);
        }
    }

    /**
     * The unique string ID of the game.
     *
     * @return the ID of the game.
     */
    public String getId() {
        return id;
    }

    /**
     * The user-presentable name of the game.
     *
     * @return the name of the game.
     */
    public String getName() {
        return name;
    }

    /**
     * Pseudonyms of the game.
     * <p/>
     * This is used in instances where we try and resolve a game from a number of options,
     * e.g. a user-entered string.
     *
     * @return pseudonyms of the game.
     */
    public Set<String> getPseudonyms() {
        return Collections.unmodifiableSet(pseudonyms);
    }

    /**
     * Does this game support the given feature?
     * 
     * @param feature the feature to test. Ignored when null.
     * @return true if the feature is supported, otherwise false.
     */
    public boolean isSupported(final GameFeature feature) {
        return feature != null && features.contains(feature);
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
        final GameType rhs = (GameType) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(name, rhs.name)
                .append(pseudonyms, rhs.pseudonyms)
                .append(features, rhs.features)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 19)
                .append(id)
                .append(name)
                .append(pseudonyms)
                .append(features)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(name)
                .append(pseudonyms)
                .append(features)
                .toString();
    }
}
