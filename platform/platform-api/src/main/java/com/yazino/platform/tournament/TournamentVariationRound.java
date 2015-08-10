package com.yazino.platform.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class TournamentVariationRound implements Serializable {
    private static final long serialVersionUID = -3672223520978262638L;
    private final int roundNumber;
    private final long roundEndInterval;
    private final long roundLength;
    private final BigDecimal minimumBalance;
    private final BigDecimal gameVariationTemplateId;
    private final String clientPropertiesId;
    private final String description;

    public TournamentVariationRound(final int roundNumber,
                                    final long roundEndInterval,
                                    final long roundLength,
                                    final BigDecimal gameVariationTemplateId,
                                    final String clientPropertiesId,
                                    final BigDecimal minimumBalance,
                                    final String description) {
        notNull(gameVariationTemplateId, "Game Variation Template Id may not be Null.");

        this.roundNumber = roundNumber;
        this.roundEndInterval = roundEndInterval;
        this.roundLength = roundLength;
        this.gameVariationTemplateId = gameVariationTemplateId;
        this.clientPropertiesId = clientPropertiesId;
        this.minimumBalance = minimumBalance;
        this.description = description;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public long getRoundEndInterval() {
        return roundEndInterval;
    }

    public long getRoundLength() {
        return roundLength;
    }

    public BigDecimal getGameVariationTemplateId() {
        return gameVariationTemplateId;
    }

    public String getClientPropertiesId() {
        return clientPropertiesId;
    }

    public BigDecimal getMinimumBalance() {
        return minimumBalance;
    }

    public String getDescription() {
        return description;
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
        final TournamentVariationRound rhs = (TournamentVariationRound) obj;
        return new EqualsBuilder()
                .append(roundEndInterval, rhs.roundEndInterval)
                .append(roundLength, rhs.roundLength)
                .append(roundNumber, rhs.roundNumber)
                .append(clientPropertiesId, rhs.clientPropertiesId)
                .append(gameVariationTemplateId, rhs.gameVariationTemplateId)
                .append(minimumBalance, rhs.minimumBalance)
                .append(description, rhs.description)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(roundEndInterval)
                .append(roundLength)
                .append(roundNumber)
                .append(clientPropertiesId)
                .append(gameVariationTemplateId)
                .append(minimumBalance)
                .append(description)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(roundEndInterval)
                .append(roundLength)
                .append(roundNumber)
                .append(clientPropertiesId)
                .append(gameVariationTemplateId)
                .append(minimumBalance)
                .append(description)
                .toString();
    }
}
