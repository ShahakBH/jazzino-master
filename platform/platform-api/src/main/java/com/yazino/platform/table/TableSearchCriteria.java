package com.yazino.platform.table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Encapsulates the options when selecting a table to play at.
 */
public class TableSearchCriteria implements Serializable {

    private static final long serialVersionUID = -3827425596878985933L;

    private final Set<String> tags = new HashSet<String>();
    private final Set<BigDecimal> excludedTables = new HashSet<BigDecimal>();
    private final String gameType;
    private final String variation;
    private final String clientId;

    public TableSearchCriteria(final String gameType,
                               final Set<String> tags,
                               final BigDecimal... excludedTables) {
        this(gameType, null, null, tags, excludedTables);
    }

    public TableSearchCriteria(final String gameType,
                               final String variation,
                               final String clientId,
                               final Set<String> tags,
                               final BigDecimal... excludedTables) {
        this.gameType = gameType;
        this.variation = variation;
        this.clientId = clientId;
        if (tags != null) {
            this.tags.addAll(tags);
        }
        if (excludedTables != null) {
            this.excludedTables.addAll(asList(excludedTables));
        }
    }

    public String getGameType() {
        return gameType;
    }

    public String getVariation() {
        return variation;
    }

    public String getClientId() {
        return clientId;
    }

    public Set<BigDecimal> getExcludedTables() {
        return excludedTables;
    }

    public Set<String> getTags() {
        return tags;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TableSearchCriteria that = (TableSearchCriteria) o;

        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(this.clientId, that.clientId);
        builder.append(this.gameType, that.gameType);
        builder.append(this.variation, that.variation);
        builder.append(this.tags, that.tags);
        builder.append(this.excludedTables, that.excludedTables);
        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        final HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(gameType);
        builder.append(variation);
        builder.append(clientId);
        builder.append(tags);
        builder.append(excludedTables);
        return builder.toHashCode();
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ReflectionToStringBuilder(this);
        builder.append(gameType)
                .append(variation)
                .append(clientId)
                .append(tags)
                .append(excludedTables);
        return builder.toString();
    }

}
