package com.yazino.web.domain;

import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileSummary;
import org.joda.time.DateTime;

public class PlayerProfileSummaryBuilder {

    private String gender;
    private String country;
    private DateTime dateOfBirth;

    public PlayerProfileSummaryBuilder() {
    }

    public PlayerProfileSummaryBuilder(final PlayerProfile userProfile) {
        if (userProfile != null) {
            if (userProfile.getGender() != null) {
                gender = userProfile.getGender().getId();
            }
            country = userProfile.getCountry();
            dateOfBirth = userProfile.getDateOfBirth();
        }
    }

    public PlayerProfileSummaryBuilder(final String gender, final String country, final DateTime dateOfBirth) {
        this.gender = gender;
        this.country = country;
        this.dateOfBirth = dateOfBirth;
    }

    public PlayerProfileSummaryBuilder withAvatarUrl(final String ofAvatarUrl) {
        return this;
    }

    public PlayerProfileSummaryBuilder withGender(final String ofGender) {
        this.gender = ofGender;
        return this;
    }

    public PlayerProfileSummaryBuilder withCountry(final String ofCountry) {
        this.country = ofCountry;
        return this;
    }

    public PlayerProfileSummaryBuilder withDateOfBirth(final DateTime ofDateOfBirth) {
        this.dateOfBirth = ofDateOfBirth;
        return this;
    }

    public PlayerProfileSummary build() {
        return new PlayerProfileSummary(gender, country, dateOfBirth);
    }
}
