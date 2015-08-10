package com.yazino.web.domain.email;

import com.yazino.email.EmailException;
import com.yazino.platform.player.Gender;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;

import static com.yazino.web.domain.email.ChallengeBuddiesEmailBuilder.CHALLENGE_BUDDY_TEMPLATE;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChallengeBuddiesEmailBuilderTest {

    private static final String INVITATION_MESSAGE = "Nice knowing you sukka";
    private static final BigDecimal PLAYER_ID = new BigDecimal(-1592);
    private static final BigDecimal USER_PROFILE_ID = new BigDecimal(-1633);
    private static final String REFERRAL_URL = "aReferralUrl";
    private static final String[] REFERRAL_ARRAY = new String[]{"a","ReferralUrl"};
    private static final String FIRST_EMAIL_ADDRESS = "example1@example.com";
    private static final String SECOND_EMAIL_ADDRESS = "Zoolander@blueSteel.com";
    private static final String DISPLAY_NAME = "Pickachu";
    private static final String SUBJECT_STRING = DISPLAY_NAME + " has challenged you to play at Yazino";
    private static final String fromAddress = "dis <from@your.mum>";

    private final PlayerProfileService profileService = mock(PlayerProfileService.class);
    private String gameType="BLACKJACK";

    @Before
    public void setup() {
        final PlayerProfile userProfile = new PlayerProfile(PLAYER_ID, "test@test.com", DISPLAY_NAME,
            "Test Name", Gender.MALE, "UK", "Mugato", "lastName", new DateTime(1970, 3, 3, 0, 0, 0, 0), null,
            "YAZINO", "rpxProvider", null, true);
        when(profileService.findByPlayerId(PLAYER_ID)).thenReturn(userProfile);
    }

    @Test
    public void testSendChallengeBuddiesEmailSendsMail() throws EmailException {
        final ChallengeBuddiesEmailBuilder builder = new ChallengeBuddiesEmailBuilder(REFERRAL_ARRAY, PLAYER_ID, gameType, fromAddress);
        builder.withFriendEmailAddress(FIRST_EMAIL_ADDRESS);
        EmailRequest request = builder.buildRequest(profileService);
        assertEmailRequest(request, SUBJECT_STRING, CHALLENGE_BUDDY_TEMPLATE, DISPLAY_NAME,
                REFERRAL_URL, INVITATION_MESSAGE, FIRST_EMAIL_ADDRESS);
    }

    @Test
    public void shouldUpdateFriendsEmailWhenChanges() throws Exception {
        final ChallengeBuddiesEmailBuilder builder = new ChallengeBuddiesEmailBuilder(REFERRAL_ARRAY, PLAYER_ID, gameType, fromAddress);
        builder.withFriendEmailAddress(FIRST_EMAIL_ADDRESS);
        EmailRequest request = builder.buildRequest(profileService);
        assertEmailRequest(request, SUBJECT_STRING, CHALLENGE_BUDDY_TEMPLATE, DISPLAY_NAME, REFERRAL_URL, INVITATION_MESSAGE, FIRST_EMAIL_ADDRESS);
        builder.withFriendEmailAddress(SECOND_EMAIL_ADDRESS);
        request = builder.buildRequest(profileService);
        assertEmailRequest(request, SUBJECT_STRING, CHALLENGE_BUDDY_TEMPLATE, DISPLAY_NAME, REFERRAL_URL, INVITATION_MESSAGE, SECOND_EMAIL_ADDRESS);
    }

    private static void assertEmailRequest(EmailRequest request, String subject, String template, String userName,
                                           String referral, String message, String... recipients) {
        assertEquals(new HashSet<String>(asList(recipients)), request.getAddresses());
        assertEquals(subject, request.getSubject());
        assertEquals(template, request.getTemplate());

        Map<String, Object> properties = request.getProperties();
        assertEquals(userName, properties.get(ChallengeBuddiesEmailBuilder.USER_NAME_KEY));
        assertEquals(referral, properties.get(ChallengeBuddiesEmailBuilder.TARGET_URL_KEY));
    }


}


