package com.yazino.web.domain;

import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileSummary;
import com.yazino.web.controller.profile.PlayerProfileTestBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlayerProfileSummaryBuilderTest {

    @Test
    public void shouldBuildEmptyUserProfileInfoFromNullUserProfile() {
        PlayerProfileSummary expectedUserProfileInfo = new PlayerProfileSummary();
        assertEquals(expectedUserProfileInfo, new PlayerProfileSummaryBuilder(null).build());
    }

    @Test
    public void shouldHaveCorrectGenderFromUserProfile() {
        PlayerProfile userProfile = PlayerProfileTestBuilder.create().withCountry(null).withDateOfBirth(null).asProfile();
        PlayerProfileSummary expectedUserProfileInfo = new PlayerProfileSummary(userProfile.getGender().getId(), userProfile.getCountry(), userProfile.getDateOfBirth());
        assertEquals(expectedUserProfileInfo, new PlayerProfileSummaryBuilder(userProfile).build());
    }

    @Test
    public void shouldHaveCorrectDateOfBirthFromUserProfile() {
        PlayerProfile userProfile = PlayerProfileTestBuilder.create().withCountry(null).withGender(null).asProfile();
        PlayerProfileSummary expectedUserProfileInfo = new PlayerProfileSummary(null, null, userProfile.getDateOfBirth());
        assertEquals(expectedUserProfileInfo, new PlayerProfileSummaryBuilder(userProfile).build());
    }

    @Test
    public void shouldHaveCorrectCountryFromUserProfile() {
        PlayerProfile userProfile = PlayerProfileTestBuilder.create().withDateOfBirth(null).withGender(null).asProfile();
        PlayerProfileSummary expectedUserProfileInfo = new PlayerProfileSummary(null, userProfile.getCountry(), null);
        assertEquals(expectedUserProfileInfo, new PlayerProfileSummaryBuilder(userProfile).build());
    }
}
