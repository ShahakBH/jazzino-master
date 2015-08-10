package com.yazino.game.api.facebook;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class OpenGraphActionProvider implements Serializable {
    private static final long serialVersionUID = -8144721066403473748L;

    private final Set<OpenGraphAction> openGraphActions = new HashSet<OpenGraphAction>();

    public OpenGraphActionProvider(final Set<OpenGraphAction> openGraphActions) {
        notNull(openGraphActions, "openGraphActions may not be null");

        this.openGraphActions.addAll(openGraphActions);
    }

    public OpenGraphActionProvider(final OpenGraphAction... openGraphActions) {
        notNull(openGraphActions, "openGraphActions may not be null");

        Collections.addAll(this.openGraphActions, openGraphActions);
    }

    public Set<OpenGraphAction> getOpenGraphActions() {
        return Collections.unmodifiableSet(openGraphActions);
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
        final OpenGraphActionProvider rhs = (OpenGraphActionProvider) obj;
        return new EqualsBuilder()
                .append(openGraphActions, rhs.openGraphActions)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(openGraphActions)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(openGraphActions)
                .toString();
    }
}
