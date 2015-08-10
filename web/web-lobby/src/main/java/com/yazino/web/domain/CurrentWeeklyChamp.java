package com.yazino.web.domain;

import com.yazino.platform.tournament.TrophyWinner;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

/**
 * Controls some of the data presented for the current weekly champ, currently on the page 'hallOfFame'
 */
public class CurrentWeeklyChamp {
    private TrophyWinner trophyWinner;

    public CurrentWeeklyChamp(final TrophyWinner trophyWinner) {
        this.trophyWinner = trophyWinner;
    }

    public boolean getWeeklyChampExists() {
        return this.trophyWinner != null;
    }

    public String getName() {
        if (this.trophyWinner != null) {
            return this.trophyWinner.getName();
        }
        return "";
    }

    public String getPictureUrl() {
        if (this.trophyWinner != null) {
            return this.trophyWinner.getPictureUrl();
        }
        return "";
    }

    public String getAwardTime() {
        if (this.trophyWinner != null) {
            final DateTime dt = this.trophyWinner.getAwardTime();
            if (dt != null) {
                return dt.monthOfYear().getAsText() + " " + dt.getDayOfMonth() + ", " + dt.getYear();
            }
        }
        return "";
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final CurrentWeeklyChamp rhs = (CurrentWeeklyChamp) obj;
        return new EqualsBuilder()
                .append(trophyWinner, rhs.trophyWinner)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(trophyWinner)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(trophyWinner)
                .toString();
    }
}
