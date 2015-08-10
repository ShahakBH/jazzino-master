package com.yazino.engagement;

import com.yazino.platform.messaging.Message;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleCloudMessage implements Message {

    private static final long serialVersionUID = 1L;

    private Integer appRequestId;
    private Integer appRequestTargetId;
    private BigDecimal playerId;
    private String registrationId;

    public GoogleCloudMessage() {
    }

    public GoogleCloudMessage(Integer appRequestId, Integer appRequestTargetId, BigDecimal playerId, String registrationId) {
        this.appRequestId = appRequestId;
        this.appRequestTargetId = appRequestTargetId;
        this.playerId = playerId;
        this.registrationId = registrationId;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public String getMessageType() {
        return "GoogleCloudMessage";
    }

    public Integer getAppRequestId() {
        return appRequestId;
    }

    public void setAppRequestId(Integer appRequestId) {
        this.appRequestId = appRequestId;
    }

    public Integer getAppRequestTargetId() {
        return appRequestTargetId;
    }

    public void setAppRequestTargetId(Integer appRequestTargetId) {
        this.appRequestTargetId = appRequestTargetId;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(BigDecimal playerId) {
        this.playerId = playerId;
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

        final GoogleCloudMessage rhs = (GoogleCloudMessage) obj;
        return new EqualsBuilder()
                .append(appRequestId, rhs.appRequestId)
                .append(appRequestTargetId, rhs.appRequestTargetId)
                .append(registrationId, rhs.registrationId)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(appRequestId)
                .append(appRequestTargetId)
                .append(BigDecimals.strip(playerId))
                .append(registrationId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "GoogleCloudMessage{"
                + "appRequestId=" + appRequestId
                + ", appRequestTargetId=" + appRequestTargetId
                + ", playerId=" + playerId
                + ", registrationId='" + registrationId + '\''
                + '}';
    }
}
