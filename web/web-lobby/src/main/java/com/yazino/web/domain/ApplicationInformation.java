package com.yazino.web.domain;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public class ApplicationInformation implements Serializable {
    private static final long serialVersionUID = -6059912674302779660L;

    private final String gameType;
    private final Partner partner;
    private final Platform platform;

    public ApplicationInformation(final String gameType,
                                  final Partner partner,
                                  final Platform platform) {
        notBlank(gameType, "gameType may not be null");
        notNull(partner, "partner may not be null");
        notNull(platform, "platform may not be null");

        this.gameType = gameType;
        this.partner = partner;
        this.platform = platform;
    }

    public String getGameType() {
        return gameType;
    }

    public Partner getPartner() {
        return partner;
    }

    public Platform getPlatform() {
        return platform;
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
        ApplicationInformation rhs = (ApplicationInformation) obj;
        return new EqualsBuilder()
                .append(this.gameType, rhs.gameType)
                .append(this.partner, rhs.partner)
                .append(this.platform, rhs.platform)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(gameType)
                .append(partner)
                .append(platform)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("gameType", gameType)
                .append("partner", partner)
                .append("platform", platform)
                .toString();
    }
}
