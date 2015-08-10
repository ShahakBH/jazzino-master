package com.yazino.platform.worker.message;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerVerifiedMessage implements PlatformMessage {
    private static final long serialVersionUID = 8313458328558428104L;

    private VerificationType verificationType;
    private BigDecimal playerId;

    public PlayerVerifiedMessage() {
    }

    public PlayerVerifiedMessage(final BigDecimal playerId, final VerificationType verificationType) {
        notNull(playerId, "playerId may not be null");
        notNull(verificationType, "verificationType may not be null");

        this.playerId = playerId;
        this.verificationType = verificationType;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public PlatformMessageType getMessageType() {
        return PlatformMessageType.PLAYER_VERIFIED;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public VerificationType getVerificationType() {
        return verificationType;
    }

    public void setVerificationType(final VerificationType verificationType) {
        this.verificationType = verificationType;
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
        final PlayerVerifiedMessage rhs = (PlayerVerifiedMessage) obj;
        return new EqualsBuilder()
                .append(verificationType, rhs.verificationType)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(verificationType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(verificationType)
                .toString();
    }

}
