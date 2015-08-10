package com.yazino.mobile.yaps.message;

import com.yazino.platform.messaging.Message;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerDevice implements Message {

    private static final long serialVersionUID = 8362484697266832716L;

    private final String gameType;
    private final String bundle;
    private final BigDecimal playerId;
    private final String deviceToken;

    @JsonCreator
    public PlayerDevice(@JsonProperty("gameType") final String gameType,
                        @JsonProperty("playerId") final BigDecimal playerId,
                        @JsonProperty("deviceToken") final String deviceToken,
                        @JsonProperty("bundle") final String bundle) {
        notBlank(gameType);
        notNull(playerId);
        notBlank(deviceToken);
        notBlank(bundle);
        this.gameType = gameType;
        this.playerId = playerId;
        this.deviceToken = deviceToken;
        this.bundle = bundle;
    }

    public String getGameType() {
        return gameType;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public String getBundle() {
        return bundle;
    }

    @Override
    public int hashCode() {
        final HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(BigDecimals.strip(playerId));
        builder.append(deviceToken);
        builder.append(gameType);
        builder.append(bundle);
        return builder.toHashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || !(o instanceof PlayerDevice)) {
            return false;
        }
        final PlayerDevice other = (PlayerDevice) o;
        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(deviceToken, other.deviceToken);
        builder.append(gameType, other.gameType);
        builder.append(bundle, other.bundle);
        return builder.isEquals()
                && BigDecimals.equalByComparison(playerId, other.playerId);
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).
                append("gameType", gameType).
                append("bundle", bundle).
                append("playerId", playerId).
                append("deviceToken", deviceToken).
                toString();
    }

    @Override
    public int getVersion() {
        return Message.VERSION;
    }

    @Override
    public Object getMessageType() {
        return "PlayerDevice";
    }
}
