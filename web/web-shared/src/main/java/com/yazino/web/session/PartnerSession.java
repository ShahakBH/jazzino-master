package com.yazino.web.session;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class PartnerSession implements Serializable {
    private static final long serialVersionUID = 7959460478251202626L;

    private final String referrer;
    private final String ipAddress;
    private final Partner partnerId;
    private final Platform platform;
    private final String loginUrl;

    public PartnerSession(final String referrer,
                          final String ipAddress,
                          final Partner partnerId,
                          final Platform platform,
                          final String loginUrl) {
        this.referrer = referrer;
        this.ipAddress = ipAddress;
        this.partnerId = partnerId;
        this.platform = platform;
        this.loginUrl = loginUrl;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("referrer", referrer)
                .append("ipAddress", ipAddress)
                .append("partnerId", partnerId)
                .append("platform", platform)
                .append("loginUrl", loginUrl)
                .toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof PartnerSession)) {
            return false;
        }
        final PartnerSession castOther = (PartnerSession) other;
        return new EqualsBuilder()
                .append(referrer, castOther.referrer)
                .append(ipAddress, castOther.ipAddress)
                .append(partnerId, castOther.partnerId)
                .append(platform, castOther.platform)
                .append(loginUrl, castOther.loginUrl)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(referrer)
                .append(ipAddress)
                .append(partnerId)
                .append(platform)
                .append(loginUrl)
                .toHashCode();
    }

    public String getReferrer() {
        return referrer;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Partner getPartnerId() {
        return partnerId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getLoginUrl() {
        return loginUrl;
    }
}
