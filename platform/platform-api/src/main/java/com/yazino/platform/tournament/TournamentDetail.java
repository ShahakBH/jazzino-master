package com.yazino.platform.tournament;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class TournamentDetail implements Serializable {
    private static final long serialVersionUID = -1234514741771829316L;

    private final BigDecimal tournamentId;
    private final String name;
    private final String description;
    private final String gameType;
    private final String variationTemplateName;
    private final Long millisToStart;
    private final Integer registeredPlayers;
    private final Integer registeredFriends;
    private final Boolean inProgress;
    private final Boolean playerRegistered;
    private final BigDecimal firstPrize;
    private final BigDecimal prizePool;
    private final Date gmtStartTime;
    private final BigDecimal registrationFee;

    public TournamentDetail(final BigDecimal tournamentId,
                            final String name,
                            final String gameType,
                            final String variationTemplateName,
                            final String description,
                            final Integer registeredPlayers,
                            final Integer registeredFriends,
                            final Boolean inProgress,
                            final Boolean playerRegistered,
                            final BigDecimal firstPrize,
                            final BigDecimal prizePool,
                            final BigDecimal registrationFee,
                            final Long millisToStart,
                            final Date gmtStartTime) {
        this.tournamentId = tournamentId;
        this.name = name;
        this.gameType = gameType;
        this.variationTemplateName = variationTemplateName;
        this.description = description;
        this.registeredPlayers = registeredPlayers;
        this.registeredFriends = registeredFriends;
        this.inProgress = inProgress;
        this.playerRegistered = playerRegistered;
        this.firstPrize = firstPrize;
        this.registrationFee = registrationFee;
        this.millisToStart = millisToStart;
        this.prizePool = prizePool;
        this.gmtStartTime = gmtStartTime;
    }

    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getRegisteredPlayers() {
        return registeredPlayers;
    }

    public Integer getRegisteredFriends() {
        return registeredFriends;
    }

    public Boolean getInProgress() {
        return inProgress;
    }

    public Boolean getPlayerRegistered() {
        return playerRegistered;
    }

    public BigDecimal getFirstPrize() {
        return firstPrize;
    }

    public BigDecimal getRegistrationFee() {
        return registrationFee;
    }

    public Long getMillisToStart() {
        return millisToStart;
    }

    public String getGameType() {
        return gameType;
    }

    public BigDecimal getPrizePool() {
        return prizePool;
    }

    public Date getGmtStartTime() {
        return gmtStartTime;
    }

    public String getVariationTemplateName() {
        return variationTemplateName;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
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
        final TournamentDetail rhs = (TournamentDetail) obj;
        return new EqualsBuilder()
                .append(description, rhs.description)
                .append(firstPrize, rhs.firstPrize)
                .append(inProgress, rhs.inProgress)
                .append(millisToStart, rhs.millisToStart)
                .append(name, rhs.name)
                .append(playerRegistered, rhs.playerRegistered)
                .append(registeredFriends, rhs.registeredFriends)
                .append(registeredPlayers, rhs.registeredPlayers)
                .append(registrationFee, rhs.registrationFee)
                .append(gameType, rhs.gameType)
                .append(variationTemplateName, rhs.variationTemplateName)
                .append(prizePool, rhs.prizePool)
                .isEquals()
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(description)
                .append(firstPrize)
                .append(inProgress)
                .append(millisToStart)
                .append(name)
                .append(playerRegistered)
                .append(registeredFriends)
                .append(registeredPlayers)
                .append(registrationFee)
                .append(gameType)
                .append(variationTemplateName)
                .append(BigDecimals.strip(tournamentId))
                .append(prizePool)
                .toHashCode();
    }
}
