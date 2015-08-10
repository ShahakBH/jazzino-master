package com.yazino.platform.player;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public class PasswordChangeRequest implements Serializable {
    private static final long serialVersionUID = 7241530510766766625L;

    private String currentPassword;
    private String newPassword;
    private String confirmNewPassword;
    private BigDecimal playerId;

    public PasswordChangeRequest() {
    }

    public PasswordChangeRequest(final String currentPassword,
                                 final String newPassword,
                                 final String confirmNewPassword,
                                 final BigDecimal playerId) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
        this.playerId = playerId;
    }

    public PasswordChangeRequest(final PlayerProfile playerProfile) {
        playerId = playerProfile.getPlayerId();
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(final String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(final String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(final String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public void clear() {
        this.currentPassword = null;
        this.newPassword = null;
        this.confirmNewPassword = null;
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
        final PasswordChangeRequest rhs = (PasswordChangeRequest) obj;
        return new EqualsBuilder()
                .append(currentPassword, rhs.currentPassword)
                .append(newPassword, rhs.newPassword)
                .append(confirmNewPassword, rhs.confirmNewPassword)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(currentPassword)
                .append(newPassword)
                .append(confirmNewPassword)
                .append(BigDecimals.strip(playerId))
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(currentPassword)
                .append(newPassword)
                .append(confirmNewPassword)
                .append(playerId)
                .toString();

    }
}
