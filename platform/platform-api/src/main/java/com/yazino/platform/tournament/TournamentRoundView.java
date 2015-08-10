package com.yazino.platform.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

public final class TournamentRoundView implements Serializable {
    private static final long serialVersionUID = 5301198280237494121L;

    private final int level;
    private final String description;
    private final BigDecimal minStake;
    private final String clientFile;
    private final int minutes;

    private TournamentRoundView(final int level,
                                final String description,
                                final BigDecimal minStake,
                                final String clientFile,
                                final int minutes) {
        this.level = level;
        this.description = description;
        this.minStake = minStake;
        this.clientFile = clientFile;
        this.minutes = minutes;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getMinStake() {
        return minStake;
    }

    public String getClientFile() {
        return clientFile;
    }

    public int getMinutes() {
        return minutes;
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
        final TournamentRoundView rhs = (TournamentRoundView) obj;
        return new EqualsBuilder()
                .append(level, rhs.level)
                .append(description, rhs.description)
                .append(minStake, rhs.minStake)
                .append(clientFile, rhs.clientFile)
                .append(minutes, rhs.minutes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(level)
                .append(description)
                .append(minStake)
                .append(clientFile)
                .append(minutes)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(level)
                .append(description)
                .append(minStake)
                .append(clientFile)
                .append(minutes)
                .toString();
    }

    public static class Builder {
        private int level;
        private String description;
        private BigDecimal minStake;
        private String clientFile;
        private int minutes;

        public TournamentRoundView build() {
            return new TournamentRoundView(level, description, minStake, clientFile, minutes);
        }

        public Builder minStake(final BigDecimal newMinStake) {
            this.minStake = newMinStake;
            return this;
        }

        public Builder level(final int newLevel) {
            this.level = newLevel;
            return this;
        }

        public Builder description(final String newDescription) {
            this.description = newDescription;
            return this;
        }

        public Builder clientFile(final String newClientFile) {
            this.clientFile = newClientFile;
            return this;
        }

        public Builder minutes(final int newMinutes) {
            this.minutes = newMinutes;
            return this;
        }
    }
}
