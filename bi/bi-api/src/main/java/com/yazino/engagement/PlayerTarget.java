package com.yazino.engagement;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

public class PlayerTarget {

    private final String gameType;
    private final String externalId;
    private final BigDecimal playerId;
    private final String targetToken;
    private final String bundle;
    private final Map<String, String> customData;

    public String getGameType() {
        return gameType;
    }

    public String getExternalId() {
        return externalId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    // For ios it is device token, for android it is Registration id
    public String getTargetToken() {
        return targetToken;
    }

    public String getBundle() {
        return bundle;
    }

    public Map<String, String> getCustomData() {
        return customData;
    }

    @JsonCreator
    public PlayerTarget(@JsonProperty("gameType") String gameType, @JsonProperty("externalId") String externalId,
                        @JsonProperty("playerId") BigDecimal playerId, @JsonProperty("targetToken") String targetToken,
                        @JsonProperty("bundle") String bundle,
                        @JsonProperty("customData") final Map<String, String> customData) {
        this.gameType = gameType;
        this.externalId = externalId;
        this.playerId = playerId;
        this.targetToken = targetToken;
        this.bundle = bundle;
        this.customData = customData;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.gameType)
                .append(this.externalId)
                .append(BigDecimals.strip(this.playerId))
                .append(this.targetToken)
                .append(this.bundle)
                .append(this.customData)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PlayerTarget other = (PlayerTarget) obj;
        return new EqualsBuilder()
                .append(this.gameType, other.gameType)
                .append(this.externalId, other.externalId)
                .append(this.targetToken, other.targetToken)
                .append(this.bundle, other.bundle)
                .append(this.customData, other.customData)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, other.playerId);
    }

    @Override
    public String toString() {
        return "PlayerTarget{"
                + "gameType='" + gameType + '\''
                + ", externalId='" + externalId + '\''
                + ", playerId=" + playerId
                + ", targetToken='" + targetToken + '\''
                + ", bundle='" + bundle + '\''
                + ", customData='" + customData + '\''
                + '}';
    }
}
