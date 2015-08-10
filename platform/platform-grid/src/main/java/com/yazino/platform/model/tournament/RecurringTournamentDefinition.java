package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.yazino.platform.tournament.DayPeriod;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines a recurring tournament, when it should run, how often, exclusion periods etc.
 */
@SpaceClass
public class RecurringTournamentDefinition implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(RecurringTournamentDefinition.class);

    private static final long serialVersionUID = 6021851772268244550L;

    private BigInteger id;
    private DateTime initialSignupTime;
    private Long signupPeriod;
    private Long frequency;
    private DayPeriod[] exclusionPeriods;
    private TournamentVariationTemplate tournamentVariationTemplate;
    private String tournamentName;
    private String tournamentDescription;
    private String partnerId;
    private Boolean enabled;

    public Set<DateTime> calculateSignupTimes(final Interval interval) {
        final Set<DateTime> signupTimes = new HashSet<DateTime>();

        if (interval.contains(initialSignupTime) && !excluded(initialSignupTime)) {
            signupTimes.add(initialSignupTime);
        }

        if (frequency > 0) {
            DateTime rollingTime = initialSignupTime.plus(frequency);
            while (rollingTime.isBefore(interval.getEnd())) {
                if (interval.contains(rollingTime) && !excluded(rollingTime)) {
                    signupTimes.add(rollingTime);
                }
                rollingTime = rollingTime.plus(frequency);
            }
        }

        LOG.debug("SignupTimes for [{}] are [{}]", this, signupTimes);

        return signupTimes;
    }

    private boolean excluded(final DateTime time) {
        if (exclusionPeriods == null) {
            return false;
        }
        for (DayPeriod exclusionPeriod : exclusionPeriods) {
            if (exclusionPeriod.isWithinPeriod(time)) {
                return true;
            }
        }
        return false;
    }

    @SpaceId
    public BigInteger getId() {
        return id;
    }

    public void setId(final BigInteger id) {
        this.id = id;
    }

    public Long getSignupPeriod() {
        return signupPeriod;
    }

    public void setSignupPeriod(final Long signupPeriod) {
        this.signupPeriod = signupPeriod;
    }

    public Long getFrequency() {
        return frequency;
    }

    public void setFrequency(final Long frequency) {
        this.frequency = frequency;
    }

    public DayPeriod[] getExclusionPeriods() {
        return exclusionPeriods;
    }

    public void setExclusionPeriods(final DayPeriod... exclusionPeriods) {
        this.exclusionPeriods = exclusionPeriods;
    }

    public TournamentVariationTemplate getTournamentVariationTemplate() {
        return tournamentVariationTemplate;
    }

    public void setTournamentVariationTemplate(final TournamentVariationTemplate tournamentVariationTemplate) {
        this.tournamentVariationTemplate = tournamentVariationTemplate;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    public DateTime getInitialSignupTime() {
        return initialSignupTime;
    }

    public void setInitialSignupTime(final DateTime initialSignupTime) {
        this.initialSignupTime = initialSignupTime;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(final String tournamentName) {
        this.tournamentName = tournamentName;
    }

    public String getTournamentDescription() {
        return tournamentDescription;
    }

    public void setTournamentDescription(final String tournamentDescription) {
        this.tournamentDescription = tournamentDescription;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(final String partnerId) {
        this.partnerId = partnerId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof RecurringTournamentDefinition)) {
            return false;
        }
        final RecurringTournamentDefinition that = (RecurringTournamentDefinition) obj;
        final EqualsBuilder builder = new EqualsBuilder();
        return builder.append(id, that.id)
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
        final HashCodeBuilder builder = new HashCodeBuilder();
        return builder.append(id)
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
        return ToStringBuilder.reflectionToString(this);
    }
}
