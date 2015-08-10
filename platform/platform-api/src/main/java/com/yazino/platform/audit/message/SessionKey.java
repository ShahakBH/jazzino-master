package com.yazino.platform.audit.message;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SessionKey implements Serializable {
    private static final long serialVersionUID = 2475475637433428537L;

    @Deprecated
    @JsonProperty("act")
    private BigDecimal accountId;
    @JsonProperty("id")
    private BigDecimal playerId;
    @JsonProperty("sid")
    private BigDecimal sessionId;
    @JsonProperty("ssk")
    private String sessionKey;
    @JsonProperty("ip")
    private String ipAddress;
    @JsonProperty("ref")
    private String referrer;
    @JsonProperty("platform")
    private String platform;
    @JsonProperty("loginUrl")
    private String loginUrl;
    @JsonProperty("clientContext")
    private Map<String, Object> clientContext;

    public SessionKey() {
    }

    public SessionKey(final BigDecimal sessionId,
                      final BigDecimal accountId,
                      final BigDecimal playerId,
                      final String sessionKey,
                      final String ipAddress,
                      final String referrer,
                      final String platform,
                      final String loginUrl,
                      final Map<String, Object> clientContext) {
        notNull(sessionId, "sessionId is null");
        notNull(playerId, "playerId is null");
        notNull(accountId, "accountId is null");
        notNull(sessionKey, "sessionKey is null");

        this.sessionId = sessionId;
        this.accountId = accountId;
        this.playerId = playerId;
        this.sessionKey = sessionKey;
        this.ipAddress = ipAddress;
        this.referrer = referrer;
        this.platform = platform;
        this.loginUrl = loginUrl;
        this.clientContext = clientContext;
    }


    public String getPlatform() {
        return platform;
    }

    public void setPlatform(final String platform) {
        this.platform = platform;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public void setSessionId(final BigDecimal sessionId) {
        this.sessionId = sessionId;
    }

    @Deprecated
    public BigDecimal getAccountId() {
        return accountId;
    }

    @Deprecated
    public void setAccountId(final BigDecimal accountId) {
        this.accountId = accountId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public void setSessionKey(final String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(final String referrer) {
        this.referrer = referrer;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public Map<String, Object> getClientContext() {
        return clientContext;
    }

    public void setClientContext(final Map<String, Object> clientContext) {
        this.clientContext = clientContext;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof SessionKey)) {
            return false;
        }
        final SessionKey castOther = (SessionKey) other;
        return new EqualsBuilder()
                .append(sessionId, castOther.sessionId)
                .append(sessionKey, castOther.sessionKey)
                .append(ipAddress, castOther.ipAddress)
                .append(referrer, castOther.referrer)
                .append(platform, castOther.platform)
                .append(loginUrl, castOther.loginUrl)
                .append(clientContext, castOther.clientContext)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, castOther.playerId)
                && BigDecimals.equalByComparison(accountId, castOther.accountId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(sessionId))
                .append(BigDecimals.strip(accountId))
                .append(playerId)
                .append(sessionKey)
                .append(ipAddress)
                .append(referrer)
                .append(platform)
                .append(loginUrl)
                .append(clientContext)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(sessionId)
                .append(accountId)
                .append(playerId)
                .append(sessionKey)
                .append(ipAddress)
                .append(referrer)
                .append(platform)
                .append(clientContext)
                .toString();
    }
}
