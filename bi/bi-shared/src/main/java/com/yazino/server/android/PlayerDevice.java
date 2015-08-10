package com.yazino.server.android;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.math.BigDecimal;

public class PlayerDevice {

    private final BigDecimal playerId;
    private final String deviceToken;

    public PlayerDevice(BigDecimal playerId, String deviceToken) {
        this.playerId = playerId;
        this.deviceToken = deviceToken;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getDeviceToken() {
        return deviceToken;
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

        final PlayerDevice rhs = (PlayerDevice) obj;
        return new EqualsBuilder()
                .append(playerId, rhs.playerId)
                .append(deviceToken, rhs.deviceToken)
                .isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(playerId)
                .append(deviceToken)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "PlayerDevice{"
                + "playerId=" + playerId
                + ", deviceToken='" + deviceToken + '\''
                + '}';
    }


}
