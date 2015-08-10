package com.yazino.platform.player;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class ResetPasswordResponse implements Serializable {
    private static final long serialVersionUID = 5960805718565812915L;

    private final BigDecimal playerId;
    private final String playerName;
    private final String newPassword;

    public ResetPasswordResponse() {
        playerId = null;
        playerName = null;
        newPassword = null;
    }

    public ResetPasswordResponse(final BigDecimal playerId,
                                 final String playerName,
                                 final String newPassword) {
        notNull(playerId, "playerId may not be null");

        this.playerId = playerId;
        this.playerName = playerName;
        this.newPassword = newPassword;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public boolean isSuccessful() {
        return playerId != null;
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
        final ResetPasswordResponse rhs = (ResetPasswordResponse) obj;
        return new EqualsBuilder()
                .append(playerName, rhs.playerName)
                .append(newPassword, rhs.newPassword)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(playerName)
                .append(newPassword)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(playerName)
                .append(newPassword)
                .toString();
    }
}
