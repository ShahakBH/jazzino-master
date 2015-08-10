package com.yazino.platform.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

public class RecurringTournament implements Serializable {
    private static final long serialVersionUID = 4872993815979120777L;

    private final BigInteger id;
    private final DateTime initialSignupTime;
    private final Long signupPeriod;
    private final Long frequency;
    private final List<DayPeriod> exclusionPeriods;
    private final TournamentVariationTemplate tournamentVariationTemplate;
    private final String tournamentName;
    private final String tournamentDescription;
    private final String partnerId;
    private final boolean enabled;

    public RecurringTournament(final BigInteger id,
                               final DateTime initialSignupTime,
                               final Long signupPeriod,
                               final Long frequency,
                               final List<DayPeriod> exclusionPeriods,
                               final TournamentVariationTemplate tournamentVariationTemplate,
                               final String tournamentName,
                               final String tournamentDescription,
                               final String partnerId,
                               final boolean enabled) {
        this.id = id;
        this.initialSignupTime = initialSignupTime;
        this.signupPeriod = signupPeriod;
        this.frequency = frequency;
        this.exclusionPeriods = exclusionPeriods;
        this.tournamentVariationTemplate = tournamentVariationTemplate;
        this.tournamentName = tournamentName;
        this.tournamentDescription = tournamentDescription;
        this.partnerId = partnerId;
        this.enabled = enabled;
    }

    public BigInteger getId() {
        return id;
    }

    public DateTime getInitialSignupTime() {
        return initialSignupTime;
    }

    public Long getSignupPeriod() {
        return signupPeriod;
    }

    public Long getFrequency() {
        return frequency;
    }

    public List<DayPeriod> getExclusionPeriods() {
        return exclusionPeriods;
    }

    public TournamentVariationTemplate getTournamentVariationTemplate() {
        return tournamentVariationTemplate;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public String getTournamentDescription() {
        return tournamentDescription;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof RecurringTournament)) {
            return false;
        }
        final RecurringTournament that = (RecurringTournament) obj;
        final EqualsBuilder builder = new EqualsBuilder();
        return new EqualsBuilder()
                .append(id, that.id)
                .append(initialSignupTime, that.initialSignupTime)
                .append(signupPeriod, that.signupPeriod)
                .append(frequency, that.frequency)
                .append(exclusionPeriods, that.exclusionPeriods)
                .append(tournamentVariationTemplate, that.tournamentVariationTemplate)
                .append(partnerId, that.partnerId)
                .append(tournamentName, that.tournamentName)
                .append(tournamentDescription, that.tournamentDescription)
                .append(enabled, that.enabled)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(initialSignupTime)
                .append(signupPeriod)
                .append(frequency)
                .append(exclusionPeriods)
                .append(tournamentVariationTemplate)
                .append(enabled)
                .append(partnerId)
                .append(tournamentName)
                .append(tournamentDescription)
                .hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(initialSignupTime)
                .append(signupPeriod)
                .append(frequency)
                .append(exclusionPeriods)
                .append(tournamentVariationTemplate)
                .append(enabled)
                .append(partnerId)
                .append(tournamentName)
                .append(tournamentDescription)
                .toString();
    }
}
