package com.yazino.engagement.campaign;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class AppRequestExternalReference implements Serializable {
    private static final long serialVersionUID = 1L;

    private String externalId;
    private String gameType;
    private String externalReference;

    public AppRequestExternalReference() {
    }

    public AppRequestExternalReference(final String externalId, final String gameType, final String externalReference) {
        this.externalId = externalId;
        this.gameType = gameType;
        this.externalReference = externalReference;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getGameType() {
        return gameType;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public void setExternalReference(final String externalReference) {
        this.externalReference = externalReference;
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
        final AppRequestExternalReference rhs = (AppRequestExternalReference) obj;
        return new EqualsBuilder()
                .append(externalId, rhs.externalId)
                .append(gameType, rhs.gameType)
                .append(externalReference, rhs.externalReference)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(externalId)
                .append(gameType)
                .append(externalReference)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(externalId)
                .append(gameType)
                .append(externalReference)
                .toString();
    }

}
