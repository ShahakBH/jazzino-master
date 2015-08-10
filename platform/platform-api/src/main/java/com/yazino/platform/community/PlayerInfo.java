package com.yazino.platform.community;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class PlayerInfo implements Serializable {
    private static final long serialVersionUID = 7350715774199516409L;
    private final String name;
    private final String pictureUrl;

    public PlayerInfo(final String name,
                      final String pictureUrl) {
        this.name = name;
        this.pictureUrl = pictureUrl;
    }

    public String getName() {
        return name;
    }

    public String getPictureUrl() {
        return pictureUrl;
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
        final PlayerInfo rhs = (PlayerInfo) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(pictureUrl, rhs.pictureUrl)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(name)
                .append(pictureUrl)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(name)
                .append(pictureUrl)
                .toString();
    }

}
