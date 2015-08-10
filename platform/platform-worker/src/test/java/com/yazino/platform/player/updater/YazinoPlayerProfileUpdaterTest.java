package com.yazino.platform.player.updater;

import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.community.ProfanityFilter;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileUpdateResponse;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.player.persistence.YazinoLoginDao;
import com.yazino.platform.player.util.Hasher;
import com.yazino.platform.player.util.HasherFactory;
import com.yazino.platform.player.validation.YazinoPlayerValidator;
import com.yazino.platform.reference.ReferenceService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import com.yazino.game.api.ParameterisedMessage;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class YazinoPlayerProfileUpdaterTest {

    private final PlayerService playerService = mock(PlayerService.class);
    private final YazinoPlayerValidator playerValidator = mock(YazinoPlayerValidator.class);
    private final ProfanityFilter profanityFilter = mock(ProfanityFilter.class);
    private final YazinoLoginDao loginDao = mock(YazinoLoginDao.class);
    private final PlayerProfileDao playerProfileDao = mock(PlayerProfileDao.class);
    private final CommunityService communityService = mock(CommunityService.class);
    private final ReferenceService referenceService = mock(ReferenceService.class);
    private final HasherFactory hasherFactory = mock(HasherFactory.class);
    private final Hasher hasher = mock(Hasher.class);

    private final YazinoPlayerProfileUpdater underTest = new YazinoPlayerProfileUpdater(
            playerService, loginDao, playerProfileDao,
            communityService, referenceService, playerValidator, hasherFactory);

    @Before
    public void setup() {
        when(profanityFilter.filter(any(String.class))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return (String) args[0];
            }
        });

        when(playerValidator.validate(any(String.class), any(String.class), any(PlayerProfile.class), any(String.class), any(Boolean.class)))
                .thenReturn(new HashSet<ParameterisedMessage>());
        when(hasherFactory.getPreferred()).thenReturn(hasher);
    }

    @Test
    public void theUpdaterDoesAcceptsYazinoProfiles() {
        assertThat(underTest.accepts("YAZINO"), is(equalTo(true)));
    }

    @Test
    public void theUpdaterDoesNotAcceptNullProviders() {
        assertThat(underTest.accepts(null), is(equalTo(false)));
    }

    @Test
    public void theUpdaterDoesNotAcceptFacebookProfiles() {
        assertThat(underTest.accepts("FACEBOOK"), is(equalTo(false)));
    }

    @Test
    public void theUpdaterDoesNotAcceptPlayForFunProfiles() {
        assertThat(underTest.accepts("PLAY_FOR_FUN"), is(equalTo(false)));
    }

    @Test
    public void updateFails_userCannotBeFound() throws Exception {
        PlayerProfileUpdateResponse response = underTest.update(createUserProfile("a@b.com", "TestRealName", "TestDisplayName"), "foo", "anAvatarURL");
        assertFalse(response.isSuccessful());
        assertEquals(1, response.getErrors().size());
    }

    @Test
    public void updateFails_userProfileNotValid() throws Exception {
        String email = "a@b.com";
        PlayerProfile profile = createUserProfile(email, "TestRealName", "TestDisplayName");
        when(playerValidator.validate(email, null, profile, "anAvatarURL", false)).thenReturn(nonEmptyErrorSet());
        PlayerProfileUpdateResponse response = underTest.update(profile, null, "anAvatarURL");
        assertFalse(response.isSuccessful());
        assertEquals(1, response.getErrors().size());
    }

    private static PlayerProfile createUserProfile(String email, String realName, String displayName) {
        PlayerProfile userProfile = new PlayerProfile();
        userProfile.setEmailAddress(email);
        userProfile.setRealName(realName);
        userProfile.setDisplayName(displayName);
        return userProfile;
    }

    private static Set<ParameterisedMessage> nonEmptyErrorSet() {
        Set<ParameterisedMessage> errors = new HashSet<ParameterisedMessage>();
        errors.add(new ParameterisedMessage("TEST ERROR"));
        return errors;
    }

}
