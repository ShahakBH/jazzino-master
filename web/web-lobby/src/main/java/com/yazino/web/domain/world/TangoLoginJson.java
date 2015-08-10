package com.yazino.web.domain.world;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class TangoLoginJson implements Serializable {
    private String avatarUrl;
    private String displayName;
    private String accountId;

    public TangoLoginJson() {//json
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(final String accountId) {
        this.accountId = accountId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(final String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        TangoLoginJson rhs = (TangoLoginJson) obj;
        return new EqualsBuilder()
                .append(this.avatarUrl, rhs.avatarUrl)
                .append(this.displayName, rhs.displayName)
                .append(this.accountId, rhs.accountId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(avatarUrl)
                .append(displayName)
                .append(accountId)
                .toHashCode();
    }
}
