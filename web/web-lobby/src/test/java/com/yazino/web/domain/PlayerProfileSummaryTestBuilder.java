package com.yazino.web.domain;

import org.joda.time.DateTime;

public class PlayerProfileSummaryTestBuilder {
    private static final String AVATAR_URL = "http://url.to.img/here";
    private static final String GENDER = "M";
    private static final String COUNTRY = "UK";
    private static final DateTime DATE_OF_BIRTH = new DateTime(1977, 5, 25, 12, 12, 12, 12);

    public static PlayerProfileSummaryBuilder create() {
        return new PlayerProfileSummaryBuilder().withAvatarUrl(AVATAR_URL)
                .withCountry(COUNTRY)
                .withDateOfBirth(DATE_OF_BIRTH)
                .withGender(GENDER);
    }
}
