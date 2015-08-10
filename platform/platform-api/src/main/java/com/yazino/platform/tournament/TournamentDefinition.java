package com.yazino.platform.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

public class TournamentDefinition implements Serializable {
    private static final long serialVersionUID = -1389538202394221320L;

	private final BigDecimal id;
	private final String name;
	private final TournamentVariationTemplate template;
	private final DateTime signUpStart;
	private final DateTime signUpEnd;
	private final DateTime start;
	private final TournamentStatus status;
	private final String partnerId;
	private final String description;

    public TournamentDefinition(final BigDecimal id,
                                final String name,
                                final TournamentVariationTemplate template,
                                final DateTime signUpStart,
                                final DateTime signUpEnd,
                                final DateTime start,
                                final TournamentStatus status,
                                final String partnerId,
                                final String description) {
        this.id = id;
        this.name = name;
        this.template = template;
        this.signUpStart = signUpStart;
        this.signUpEnd = signUpEnd;
        this.start = start;
        this.status = status;
        this.partnerId = partnerId;
        this.description = description;
    }

    public BigDecimal getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TournamentVariationTemplate getTemplate() {
        return template;
    }

    public DateTime getSignUpStart() {
        return signUpStart;
    }

    public DateTime getSignUpEnd() {
        return signUpEnd;
    }

    public DateTime getStart() {
        return start;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public String getPartnerId() {
        return partnerId;
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
        final TournamentDefinition rhs = (TournamentDefinition) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(name, rhs.name)
                .append(template, rhs.template)
                .append(signUpStart, rhs.signUpStart)
                .append(signUpEnd, rhs.signUpEnd)
                .append(start, rhs.start)
                .append(status, rhs.status)
                .append(partnerId, rhs.partnerId)
                .append(description, rhs.description)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(id)
                .append(name)
                .append(template)
                .append(signUpStart)
                .append(signUpEnd)
                .append(start)
                .append(status)
                .append(partnerId)
                .append(description)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(name)
                .append(template)
                .append(signUpStart)
                .append(signUpEnd)
                .append(start)
                .append(status)
                .append(partnerId)
                .append(description)
                .toString();
    }
}
