package com.yazino.platform.tournament;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public final class TournamentViewDetails implements Serializable {

    private static final long serialVersionUID = 5164368083688249410L;

    public enum Status {
        CLOSED, SETTLED, FINISHED, WAITING_FOR_CLIENTS, ON_BREAK, RUNNING,
        REGISTERING, ANNOUNCED, CANCELLING, CANCELLED, ERROR
    }

    private final BigDecimal tournamentId;
    private final String name;
    private final String description;
    private final String startTime;
    private final Status status;
    private final BigDecimal entryFee;
    private final BigDecimal prizePool;
    private final Long millisTillStart;
    private final Integer playersRemaining;
    private final Integer level;
    private final Long levelEndsInMillis;
    private final Long nextBreakInMillis;
    private final Integer nextBreakLengthInMinutes;
    private final int playersRegistered;
    private final int maxPlayers;
    private final String gameType;
    private final String variationTemplateName;


    private TournamentViewDetails(final BigDecimal tournamentId,
                                  final String name,
                                  final String description,
                                  final String gameType,
                                  final String variationTemplateName,
                                  final String startTime,
                                  final Status status,
                                  final BigDecimal entryFee,
                                  final BigDecimal prizePool,
                                  final Long millisTillStart,
                                  final Integer playersRemaining,
                                  final Integer level,
                                  final Long levelEndsInMillis,
                                  final Long nextBreakInMillis,
                                  final Integer nextBreakLengthInMinutes,
                                  final int playersRegistered,
                                  final int maxPlayers) {
        this.tournamentId = tournamentId;
        this.name = name;
        this.description = description;
        this.gameType = gameType;
        this.variationTemplateName = variationTemplateName;
        this.startTime = startTime;
        this.status = status;
        this.entryFee = entryFee;
        this.prizePool = prizePool;
        this.millisTillStart = millisTillStart;
        this.playersRemaining = playersRemaining;
        this.level = level;
        this.levelEndsInMillis = levelEndsInMillis;
        this.nextBreakInMillis = nextBreakInMillis;
        this.nextBreakLengthInMinutes = nextBreakLengthInMinutes;
        this.playersRegistered = playersRegistered;
        this.maxPlayers = maxPlayers;
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

    public String getStartTime() {
        return startTime;
    }

    public Status getStatus() {
        return status;
    }

    public BigDecimal getEntryFee() {
        return entryFee;
    }

    public BigDecimal getPrizePool() {
        return prizePool;
    }

    public Long getMillisTillStart() {
        return millisTillStart;
    }

    public Integer getPlayersRemaining() {
        return playersRemaining;
    }

    public Integer getLevel() {
        return level;
    }

    public Long getLevelEndsInMillis() {
        return levelEndsInMillis;
    }

    public Long getNextBreakInMillis() {
        return nextBreakInMillis;
    }

    public Integer getNextBreakLengthInMinutes() {
        return nextBreakLengthInMinutes;
    }

    public int getPlayersRegistered() {
        return playersRegistered;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getGameType() {
        return gameType;
    }

    public String getVariationTemplateName() {
        return variationTemplateName;
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
        final TournamentViewDetails rhs = (TournamentViewDetails) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(description, rhs.description)
                .append(gameType, rhs.gameType)
                .append(variationTemplateName, rhs.variationTemplateName)
                .append(startTime, rhs.startTime)
                .append(status, rhs.status)
                .append(entryFee, rhs.entryFee)
                .append(prizePool, rhs.prizePool)
                .append(millisTillStart, rhs.millisTillStart)
                .append(playersRemaining, rhs.playersRemaining)
                .append(level, rhs.level)
                .append(levelEndsInMillis, rhs.levelEndsInMillis)
                .append(nextBreakInMillis, rhs.nextBreakInMillis)
                .append(nextBreakLengthInMinutes, rhs.nextBreakLengthInMinutes)
                .append(playersRegistered, rhs.playersRegistered)
                .append(maxPlayers, rhs.maxPlayers)
                .isEquals()
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tournamentId))
                .append(name)
                .append(description)
                .append(gameType)
                .append(variationTemplateName)
                .append(startTime)
                .append(status)
                .append(entryFee)
                .append(prizePool)
                .append(millisTillStart)
                .append(playersRemaining)
                .append(level)
                .append(levelEndsInMillis)
                .append(nextBreakInMillis)
                .append(nextBreakLengthInMinutes)
                .append(playersRegistered)
                .append(maxPlayers)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(tournamentId)
                .append(name)
                .append(description)
                .append(gameType)
                .append(variationTemplateName)
                .append(startTime)
                .append(status)
                .append(entryFee)
                .append(prizePool)
                .append(millisTillStart)
                .append(playersRemaining)
                .append(level)
                .append(levelEndsInMillis)
                .append(nextBreakInMillis)
                .append(nextBreakLengthInMinutes)
                .append(playersRegistered)
                .append(maxPlayers)
                .toString();
    }

    public static class Builder {

        private BigDecimal tournamentId;
        private String name;
        private String description;
        private String gameType;
        private String variationTemplateName;
        private String startTime;
        private Status status;
        private BigDecimal entryFee;
        private BigDecimal prizePool;
        private Long millisTillStart;
        private Integer playersRemaining;
        private Integer level;
        private Long levelEndsInMillis;
        private Long nextBreakInMillis;
        private Integer nextBreakLengthInMinutes;
        private int playersRegistered;
        private int maxPlayers;

        public Builder tournamentId(final BigDecimal newTournamentId) {
            this.tournamentId = newTournamentId;
            return this;
        }

        public TournamentViewDetails build() {
            return new TournamentViewDetails(tournamentId, name, description, gameType, variationTemplateName, startTime,
                    status, entryFee, prizePool, millisTillStart, playersRemaining, level, levelEndsInMillis, nextBreakInMillis,
                    nextBreakLengthInMinutes, playersRegistered, maxPlayers);
        }

        public Builder name(final String newName) {
            this.name = newName;
            return this;
        }

        public Builder status(final Status newStatus) {
            this.status = newStatus;
            return this;
        }

        public Builder description(final String newDescription) {
            this.description = newDescription;
            return this;
        }

        public Builder startTime(final String newStartTime) {
            this.startTime = newStartTime;
            return this;
        }

        public Builder playersRegistered(final int newPlayersRegistered) {
            this.playersRegistered = newPlayersRegistered;
            return this;
        }

        public Builder entryFee(final BigDecimal newEntryFee) {
            this.entryFee = newEntryFee;
            return this;
        }

        public Builder prizePool(final BigDecimal newPrizePool) {
            this.prizePool = newPrizePool;
            return this;
        }

        public Builder maxPlayers(final int newMaxPlayers) {
            this.maxPlayers = newMaxPlayers;
            return this;
        }

        public Builder millisTillStart(final Long newMillisTillStart) {
            this.millisTillStart = newMillisTillStart;
            return this;
        }

        public Builder playersRemaining(final Integer newPlayersRemaining) {
            this.playersRemaining = newPlayersRemaining;
            return this;
        }

        public Builder level(final Integer newLevel) {
            this.level = newLevel;
            return this;
        }

        public Builder nextBreakInMillis(final Long newNextBreakInMillis) {
            this.nextBreakInMillis = newNextBreakInMillis;
            return this;
        }

        public Builder nextBreakLengthInMinutes(final Integer newNextBreakLengthInMinutes) {
            this.nextBreakLengthInMinutes = newNextBreakLengthInMinutes;
            return this;
        }

        public Builder levelsEndIn(final Long newLevelEndsInMillis) {
            this.levelEndsInMillis = newLevelEndsInMillis;
            return this;
        }

        public Builder gameType(final String newGameType) {
            this.gameType = newGameType;
            return this;
        }

        public Builder variationTemplateName(final String newVariationTemplateName) {
            this.variationTemplateName = newVariationTemplateName;
            return this;
        }
    }
}
