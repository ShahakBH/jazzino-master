package com.yazino.web.domain;


import com.yazino.platform.player.PlayerProfile;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class DisplayName implements Serializable {
    private static final long serialVersionUID = 7241530510766766625L;

    private String displayName;

    public DisplayName() {
    }

    public DisplayName(final PlayerProfile userProfile) {
        this(userProfile.getDisplayName());
    }

    public DisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
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
        final DisplayName rhs = (DisplayName) obj;
        return new EqualsBuilder()
                .append(displayName, rhs.displayName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(displayName)
                .toHashCode();
    }

}
