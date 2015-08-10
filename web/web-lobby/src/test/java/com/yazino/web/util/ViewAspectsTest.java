package com.yazino.web.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ViewAspectsTest {

    @Test
    public void editProfileNotAvailableForFacebook() {
        final ViewAspectConfiguration conf = createAspectLimitingPartner("EDIT_PROFILE", "YAZINO");
        ViewAspects unit = new ViewAspects(Arrays.asList(conf), "BLACKJACK", "FACEBOOK");
        assertFalse(unit.supports("EDIT_PROFILE"));
    }

    @Test
    public void editProfileIsAvailableIfThereIsNoConfigurationForAspect() {
        final ViewAspectConfiguration conf = createAspectLimitingPartner("ACHIEVEMENTS", "YAZINO");

        ViewAspects unit = new ViewAspects(Arrays.asList(conf), "BLACKJACK", "FACEBOOK");
        assertTrue(unit.supports("EDIT_PROFILE"));
    }

    @Test
    public void achievementsNotAvailableForSlots() {
        final ViewAspectConfiguration conf = createAspectLimitingGameType("ACHIEVEMENTS", "BLACKJACK", "POKER", "ROULETTE");
        ViewAspects unit = new ViewAspects(Arrays.asList(conf), "SLOTS", "FACEBOOK");
        assertFalse(unit.supports("ACHIEVEMENTS"));
    }

    @Test
    public void achievementAvailableForBlackjack() {
        final ViewAspectConfiguration conf = createAspectLimitingGameType("ACHIEVEMENTS", "BLACKJACK", "POKER", "ROULETTE");
        ViewAspects unit = new ViewAspects(Arrays.asList(conf), "BLACKJACK", "FACEBOOK");
        assertTrue(unit.supports("ACHIEVEMENTS"));
    }

    @Test
    public void supportsViewForGivenGametype_isTrue() {
        final ViewAspectConfiguration conf = createAspectLimitingGameType("ACHIEVEMENTS", "BLACKJACK", "POKER", "ROULETTE");
        ViewAspects unit = new ViewAspects(Arrays.asList(conf), "BLACKJACK", "FACEBOOK");
        assertTrue(unit.supports("ACHIEVEMENTS", "POKER"));
    }

    @Test
    public void supportsViewForGivenGametype_isFalse() {
        final ViewAspectConfiguration conf = createAspectLimitingGameType("ACHIEVEMENTS", "BLACKJACK", "POKER", "ROULETTE");
        ViewAspects unit = new ViewAspects(Arrays.asList(conf), "BLACKJACK", "FACEBOOK");
        assertFalse(unit.supports("ACHIEVEMENTS", "NOT IN SUPPORTED GAME TYPES"));
    }

    private ViewAspectConfiguration createAspectLimitingGameType(String aspectName, String... gameTypes) {
        final ViewAspectConfiguration result = new ViewAspectConfiguration(aspectName);
        result.setRequiredGameTypes(Arrays.asList(gameTypes));
        return result;
    }

    @Test
    public void supportMultipleAspects() {

        final ViewAspectConfiguration editProfile = createAspectLimitingPartner("EDIT_PROFILE", "YAZINO");
        final ViewAspectConfiguration achievements = createAspectLimitingPartner("ACHIEVEMENTS", "YAZINO");
        ViewAspects unit = new ViewAspects(Arrays.asList(editProfile, achievements), "BLACKJACK", "FACEBOOK");
        assertFalse(unit.supports("EDIT_PROFILE"));
        assertFalse(unit.supports("ACHIEVEMENTS"));
    }

    @Test
    public void linkForSlotsGameIsReturnedAsConfigured() {
        final ViewAspectConfiguration conf = new ViewAspectConfiguration("EDIT_PROFILE_LINK");
        Map<String, String> links = new HashMap<String, String>();
        links.put("SLOTS", "/userDetails");
        conf.setLinksFor(links);


        ViewAspects unit = new ViewAspects(Arrays.asList(conf), "SLOTS", "YAZINO");

        assertEquals(unit.linkFor("EDIT_PROFILE_LINK", "SLOTS"), "/userDetails");
    }


    private ViewAspectConfiguration createAspectLimitingPartner(String aspectName, String partnerId) {
        final ViewAspectConfiguration result = new ViewAspectConfiguration(aspectName);
        result.setRequiredPartner(partnerId);
        return result;
    }
}
