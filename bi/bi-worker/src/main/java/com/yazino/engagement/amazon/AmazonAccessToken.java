package com.yazino.engagement.amazon;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

public class AmazonAccessToken {
    private final String token;
    private final DateTime expireTime;
    private final String scope;
    private final String tokenType;


    public AmazonAccessToken(final String token, final DateTime expireTime, final String scope, final String tokenType) {
        this.token = token;
        this.expireTime = expireTime;
        this.scope = scope;
        this.tokenType = tokenType;
    }

    public String getToken() {
        return token;
    }

    public DateTime getExpireTime() {
        return expireTime;
    }

    public String getScope() {
        return scope;
    }

    public String getTokenType() {
        return tokenType;
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
        AmazonAccessToken rhs = (AmazonAccessToken) obj;
        return new EqualsBuilder()
                .append(this.token, rhs.token)
                .append(this.expireTime, rhs.expireTime)
                .append(this.scope, rhs.scope)
                .append(this.tokenType, rhs.tokenType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(token)
                .append(expireTime)
                .append(scope)
                .append(tokenType)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("token", token)
                .append("expireTime", expireTime)
                .append("scope", scope)
                .append("tokenType", tokenType)
                .toString();
    }
}
