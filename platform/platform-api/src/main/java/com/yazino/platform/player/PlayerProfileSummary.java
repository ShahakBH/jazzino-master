package com.yazino.platform.player;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;

public class PlayerProfileSummary implements Serializable {
    private static final long serialVersionUID = 7241530510766766625L;

    private String gender;
    private String country;
    private DateTime dateOfBirth;

    public PlayerProfileSummary() {
    }

    public PlayerProfileSummary(final String gender,
                                final String country,
                                final DateTime dateOfBirth) {
        this.gender = gender;
        this.country = country;
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public DateTime getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final DateTime dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
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
        final PlayerProfileSummary rhs = (PlayerProfileSummary) obj;
        return new EqualsBuilder()
                .append(gender, rhs.gender)
                .append(country, rhs.country)
                .append(dateOfBirth, rhs.dateOfBirth)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(gender)
                .append(country)
                .append(dateOfBirth)
                .toHashCode();
    }
}
