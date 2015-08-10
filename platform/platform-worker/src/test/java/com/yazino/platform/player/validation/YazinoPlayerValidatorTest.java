package com.yazino.platform.player.validation;

import com.yazino.platform.community.ProfanityFilter;
import com.yazino.platform.player.Gender;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.persistence.YazinoLoginDao;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.ParameterisedMessage;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YazinoPlayerValidatorTest {
    private final String VALID_EMAIL = "email@email.com";
    private final String VALID_PASSWORD = "12345678";
    private final String VALID_DISPLAY_NAME = "Jack Jack";
    private final String VALID_PICTURE_LOCATION = "pictureLocation";
    private final String VALID_REAL_NAME = "Jack";
    private final Gender VALID_GENDER = Gender.MALE;
    private final String VALID_COUNTRY = "GBR";

    private ProfanityFilter profanityFilter = mock(ProfanityFilter.class);
    private YazinoLoginDao yazinoLoginDao = mock(YazinoLoginDao.class);

    private YazinoPlayerValidator underTest;

    @Before
    public void setUp() {
        when(profanityFilter.filter(VALID_DISPLAY_NAME)).thenReturn(VALID_DISPLAY_NAME);
        underTest = new YazinoPlayerValidator(yazinoLoginDao, profanityFilter);
    }

    @Test
    public void validateReturnsErrorsForAllMissingFields() {
        Set<ParameterisedMessage> expected = new HashSet<ParameterisedMessage>(
                Arrays.asList(
                        msg("Email must be entered"),
                        msg("Display name must be entered"),
                        msg("Avatar must be selected"),
                        msg("Password must be entered")
                )
        );
        Set<ParameterisedMessage> response = underTest.validate(null, null, new PlayerProfile(), null, true);
        assertEquals(expected, response);
    }

    @Test
    public void validateReturnsErrorsForAllMissingCanIgnorePasswordCheck() {
        Set<ParameterisedMessage> response = underTest.validate(null, null, new PlayerProfile(), VALID_PICTURE_LOCATION, false);
        assertFalse(response.contains(msg("Password must be entered")));
    }

    @Test
    public void validateReturnsErrorWhenFieldValueTooShort() {
        PlayerProfile profile = new PlayerProfile(VALID_EMAIL, VALID_DISPLAY_NAME,
                VALID_REAL_NAME, VALID_GENDER, VALID_COUNTRY, null, null, null, "referralId", "providerId",
                "rpxId", "externalId", true);
        Set<ParameterisedMessage> expected = new HashSet<ParameterisedMessage>();
        expected.add(msg("Password must be 5-20 characters long"));
        final Set<ParameterisedMessage> response = underTest.validate(VALID_EMAIL, "ps", profile, VALID_PICTURE_LOCATION, true);
        assertEquals(expected, response);
    }

    @Test
    public void validateReturnsErrorWhenFieldValueTooLong() {
        PlayerProfile profile = new PlayerProfile(VALID_EMAIL, VALID_DISPLAY_NAME,
                VALID_REAL_NAME, VALID_GENDER, VALID_COUNTRY, null, null, null, "referralId", "providerId",
                "rpxId", "externalId", true);
        Set<ParameterisedMessage> expected = new HashSet<ParameterisedMessage>();
        expected.add(msg("Password must be 5-20 characters long"));
        final Set<ParameterisedMessage> response =
                underTest.validate(VALID_EMAIL, "ps12345678901234567890123456789012345678901234567890",
                        profile, VALID_PICTURE_LOCATION, true);
        assertEquals(expected, response);
    }

    @Test
    public void validateReturnsErrorForNonAlphanumericDisplayName() {
        when(profanityFilter.filter("Jack* Jones")).thenReturn("Jack* Jones");
        PlayerProfile profile = new PlayerProfile(VALID_EMAIL, "Jack* Jones", VALID_REAL_NAME,
                VALID_GENDER, VALID_COUNTRY, null, null, null, "referralId", "providerId", "rpxId", "externalId", true);
        Set<ParameterisedMessage> expected = new HashSet<ParameterisedMessage>();
        expected.add(msg("Display name must be alphanumeric"));
        final Set<ParameterisedMessage> response =
                underTest.validate(VALID_EMAIL, VALID_PASSWORD, profile, VALID_PICTURE_LOCATION, true);
        assertEquals(expected, response);
    }

    @Test
    public void validateReturnsErrorForAProfaneDisplayName() {
        when(profanityFilter.filter("BadSport")).thenReturn("BadS***t");
        PlayerProfile profile = new PlayerProfile(VALID_EMAIL, "BadSport", VALID_REAL_NAME,
                VALID_GENDER, VALID_COUNTRY, null, null, null, "referralId", "providerId", "rpxId", "externalId", true);
        Set<ParameterisedMessage> expected = new HashSet<ParameterisedMessage>();
        expected.add(msg("Display name is offensive"));
        final Set<ParameterisedMessage> response =
                underTest.validate(VALID_EMAIL, VALID_PASSWORD, profile, VALID_PICTURE_LOCATION, true);
        assertEquals(expected, response);
    }

    @Test
    public void validateReturnsErrorForInvalidEmailFormat() {
        PlayerProfile profile = new PlayerProfile("invalid", VALID_DISPLAY_NAME,
                VALID_REAL_NAME, VALID_GENDER, VALID_COUNTRY, null, null, null, "referralId", "providerId",
                "rpxId", "externalId", true);
        Set<ParameterisedMessage> expected = new HashSet<ParameterisedMessage>();
        expected.add(msg("E-mail address does not appear to be valid"));
        final Set<ParameterisedMessage> response =
                underTest.validate("invalid", VALID_PASSWORD, profile, VALID_PICTURE_LOCATION, true);
        assertEquals(expected, response);
    }

    @Test
    public void validateAllowsPlusCharacterInLocalPartOfEmailAddress() {
        String validAddress = "valid+address@test.com";
        PlayerProfile profile = new PlayerProfile(validAddress, VALID_DISPLAY_NAME,
                VALID_REAL_NAME, VALID_GENDER, VALID_COUNTRY, null, null, null, "referralId", "providerId",
                "rpxId", "externalId", true);
        final Set<ParameterisedMessage> response = underTest.validate(validAddress, VALID_PASSWORD, profile, VALID_PICTURE_LOCATION, true);
        assertEquals(Collections.emptySet(), response);
    }

    @Test
    public void validateReturnsNoErrorsForValidEmailFormat() {
        PlayerProfile profile = new PlayerProfile("mel_@test.com", VALID_DISPLAY_NAME,
                VALID_REAL_NAME, VALID_GENDER, VALID_COUNTRY, null, null, null, "referralId", "providerId",
                "rpxId", "externalId", true);
        Set<ParameterisedMessage> response = underTest.validate("mel_@test.com", VALID_PASSWORD, profile, VALID_PICTURE_LOCATION, true);
        assertEquals(0, response.size());
    }

    private ParameterisedMessage msg(final String message, final String... args) {
        return new ParameterisedMessage(message, args);
    }
}
