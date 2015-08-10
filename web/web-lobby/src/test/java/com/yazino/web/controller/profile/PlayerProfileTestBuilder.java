package com.yazino.web.controller.profile;

import com.yazino.platform.player.Gender;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileStatus;
import org.joda.time.DateTime;

import java.math.BigDecimal;

public class PlayerProfileTestBuilder {

    public static final BigDecimal playerId = BigDecimal.TEN;
    public static final String emailAddress = "emailAddress@emailAddress.com";
    public static final String displayName = "displayName";
    public static final String realName = "realName";
    public static final Gender gender = Gender.OTHER;
    public static final String country = "country";
    public static final String firstName = "firstName";
    public static final String lastName = "lastName";
    public static final DateTime dateOfBirth = new DateTime(1337);
    public static final String referralIdentifier = "referralIdentifier";
    public static final String providerName = "providerName";
    public static final String rpxProvider = "rpxProvider";
    public static final String externalId = "externalId";
    public static final PlayerProfileStatus status = PlayerProfileStatus.ACTIVE;
    public static final boolean syncProfile = false;

    public static PlayerProfile.PlayerProfileBuilder create() {
        return PlayerProfile.withPlayerId(playerId)
                .withEmailAddress(emailAddress)
                .withDisplayName(displayName)
                .withRealName(realName)
                .withGender(gender)
                .withCountry(country)
                .withFirstName(firstName)
                .withLastName(lastName)
                .withDateOfBirth(dateOfBirth)
                .withReferralIdentifier(referralIdentifier)
                .withProviderName(providerName)
                .withRpxProvider(rpxProvider)
                .withExternalId(externalId)
                .withStatus(status)
                .withSyncProfile(syncProfile);
    }
}
