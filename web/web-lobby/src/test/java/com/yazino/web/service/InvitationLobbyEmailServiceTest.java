package com.yazino.web.service;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.email.EmailValidationService;
import com.yazino.platform.invitation.InvitationService;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.web.domain.email.ChallengeBuddiesEmailBuilder;
import com.yazino.web.domain.email.InviteFriendsEmailDetails;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class InvitationLobbyEmailServiceTest {

    private static final String MESSAGE = "message";
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final String REFERRAL_URL = "REF";
    private static final String GAME_TYPE = "gameType";
    private static final String SOURCE = "source";
    private static final String EMAIL_ADDRESS_1 = "email1@example.com";
    private static final String EMAIL_ADDRESS_2 = "email2@example.com";
    private static final String USER_IP_ADDRESS = "192.168.1.1";
    private static final DateTime NOW = new DateTime(2012, 7, 19, 16, 15, 30, 500);

    public static final String INVALID_EMAIL_ADDRESS = "invalid";
    private static final String[] REFERRAL_ARRAY = new String[]{"R", "EF"};
    private QuietPlayerEmailer emailer;
    private EmailValidationService emailValidationService;
    private PlayerProfileService playerProfileService;
    private PlayerProfile playerProfile;
    private InvitationLimiter limiter;
    private InvitationService invitationService;
    private InvitationLobbyEmailService underTest;
    private NoTrackingService trackingService;
    private final String sender = "from@your.mum";

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(NOW.getMillis());
        emailer = mock(QuietPlayerEmailer.class);
        emailValidationService = mock(EmailValidationService.class);
        playerProfileService = mock(PlayerProfileService.class);
        playerProfile = mock(PlayerProfile.class);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getDisplayName()).thenReturn("DISPLAY_NAME");
        invitationService = mock(InvitationService.class);
        trackingService = mock(NoTrackingService.class);
        limiter = mock(InvitationLimiter.class);
        final YazinoConfiguration yazinoConfiguration = mock(YazinoConfiguration.class);
        when(limiter.canSendInvitations(anyInt(), any(BigDecimal.class), anyString())).thenReturn(true);
        underTest = new InvitationLobbyEmailService(emailer, emailValidationService, playerProfileService, invitationService,
                trackingService, limiter, yazinoConfiguration);
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldSendInvitationEmails() {
        String[] emailAddresses = asArray(EMAIL_ADDRESS_1, EMAIL_ADDRESS_2);
        when(emailValidationService.validate(anyString())).thenReturn(true);
        when(playerProfileService.findByEmailAddresses(emailAddresses))
                .thenReturn(Collections.<String, BigDecimal>emptyMap());
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);


        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);

        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, emailAddresses, builder, false, USER_IP_ADDRESS);

        verify(invitationService).sendEmailInvitation(PLAYER_ID, playerProfile.getDisplayName(), EMAIL_ADDRESS_1,
                MESSAGE, builder.getCallToActionUrl(), new DateTime(), GAME_TYPE, SOURCE);
        verify(invitationService).sendEmailInvitation(PLAYER_ID, playerProfile.getDisplayName(), EMAIL_ADDRESS_2,
                MESSAGE, builder.getCallToActionUrl(), new DateTime(), GAME_TYPE, SOURCE);
    }

    @Test
    public void shouldNotSendEmailForInvalidEmail() {
        String[] emailAddresses = asArray(EMAIL_ADDRESS_1, INVALID_EMAIL_ADDRESS);
        when(emailValidationService.validate(EMAIL_ADDRESS_1)).thenReturn(true);
        when(emailValidationService.validate(INVALID_EMAIL_ADDRESS)).thenReturn(false);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getDisplayName()).thenReturn("DISPLAY_NAME");
        when(playerProfileService.findByEmailAddresses(emailAddresses))
                .thenReturn(Collections.<String, BigDecimal>emptyMap());

        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);

        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, emailAddresses, builder, false, USER_IP_ADDRESS);

        verify(invitationService).sendEmailInvitation(PLAYER_ID, playerProfile.getDisplayName(), EMAIL_ADDRESS_1,
                MESSAGE, builder.getCallToActionUrl(), new DateTime(), GAME_TYPE, SOURCE);
        verify(invitationService, never()).sendEmailInvitation(PLAYER_ID, playerProfile.getDisplayName(), INVALID_EMAIL_ADDRESS,
                MESSAGE, builder.getCallToActionUrl(), new DateTime(), GAME_TYPE, SOURCE);
    }

    @Test
    public void shouldNotSendEmailForAlreadyRegisteredEmail() {
        String[] emailAddresses = asArray(EMAIL_ADDRESS_1);
        when(emailValidationService.validate(anyString())).thenReturn(true);
        when(playerProfileService.findByEmailAddresses(emailAddresses))
                .thenReturn(Collections.singletonMap(EMAIL_ADDRESS_1, BigDecimal.ZERO));
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getDisplayName()).thenReturn("DISPLAY_NAME");

        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);

        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, emailAddresses, builder, false, USER_IP_ADDRESS);

        verify(invitationService, never()).sendEmailInvitation(PLAYER_ID, playerProfile.getDisplayName(), EMAIL_ADDRESS_1,
                MESSAGE, builder.getCallToActionUrl(), new DateTime(), GAME_TYPE, SOURCE);
    }

    @Test
    public void shouldNotSendAnyEmailIfSomeAreInvalidAndServiceIsConfiguredTo() {
        String[] emailAddresses = asArray(EMAIL_ADDRESS_1, INVALID_EMAIL_ADDRESS, EMAIL_ADDRESS_2);
        when(emailValidationService.validate(EMAIL_ADDRESS_1)).thenReturn(true);
        when(emailValidationService.validate(EMAIL_ADDRESS_2)).thenReturn(true);
        when(emailValidationService.validate(INVALID_EMAIL_ADDRESS)).thenReturn(false);
        when(playerProfileService.findByEmailAddresses(emailAddresses))
                .thenReturn(Collections.singletonMap(EMAIL_ADDRESS_2, BigDecimal.ZERO));

        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);

        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, emailAddresses, builder, true, USER_IP_ADDRESS);

        verifyZeroInteractions(invitationService);
    }


    @Test
    public void sendEmailsAsynchronouslyShouldReturnFalseIfPlayerHasSentTooManyEmails() {
        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);
        when(limiter.canSendInvitations(anyInt(), any(BigDecimal.class), eq(USER_IP_ADDRESS))).thenReturn(false);

        final Boolean actualResult = underTest.sendEmailsAsynchronouslyWithoutValidating(PLAYER_ID, GAME_TYPE, SOURCE, asArray(EMAIL_ADDRESS_1, EMAIL_ADDRESS_2), builder, USER_IP_ADDRESS);

        assertFalse(actualResult);
    }

    @Test
    public void shouldNotTrackInvitationSentForInvalidEmail() {
        String[] emailAddresses = asArray(INVALID_EMAIL_ADDRESS);
        when(emailValidationService.validate(INVALID_EMAIL_ADDRESS)).thenReturn(false);
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getDisplayName()).thenReturn("DISPLAY_NAME");
        when(playerProfileService.findByEmailAddresses(emailAddresses))
                .thenReturn(Collections.<String, BigDecimal>emptyMap());

        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);

        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, emailAddresses, builder, false, USER_IP_ADDRESS);

        verify(invitationService, never()).invitationSent(PLAYER_ID, INVALID_EMAIL_ADDRESS, InvitationSource.EMAIL, NOW, GAME_TYPE,
                SOURCE);
    }

    @Test
    public void shouldNotTrackInvitationSentForAlreadyRegisteredEmail() {
        String[] emailAddresses = asArray(EMAIL_ADDRESS_1);
        when(emailValidationService.validate(anyString())).thenReturn(true);
        when(playerProfileService.findByEmailAddresses(emailAddresses))
                .thenReturn(Collections.singletonMap(EMAIL_ADDRESS_1, BigDecimal.ZERO));
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getDisplayName()).thenReturn("DISPLAY_NAME");

        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);

        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, emailAddresses, builder, false, USER_IP_ADDRESS);

        verify(invitationService, never()).invitationSent(PLAYER_ID, EMAIL_ADDRESS_1, InvitationSource.EMAIL, NOW, GAME_TYPE,
                SOURCE);
    }

    @Test
    public void shouldReportNumberOfEmailsSent() {
        String[] emailAddresses = asArray(EMAIL_ADDRESS_1, EMAIL_ADDRESS_2, INVALID_EMAIL_ADDRESS);
        when(emailValidationService.validate(EMAIL_ADDRESS_1)).thenReturn(true);
        when(emailValidationService.validate(INVALID_EMAIL_ADDRESS)).thenReturn(false);
        when(playerProfileService.findByEmailAddresses(emailAddresses))
                .thenReturn(Collections.singletonMap(EMAIL_ADDRESS_2, BigDecimal.ZERO));
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getDisplayName()).thenReturn("DISPLAY_NAME");

        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);

        InvitationSendingResult sendingResult =
                underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, emailAddresses, builder, false, USER_IP_ADDRESS);

        assertEquals(1, sendingResult.getSuccessful());
    }

    @Test
    public void shouldReportRejections() {
        String[] emailAddresses = asArray(EMAIL_ADDRESS_1, EMAIL_ADDRESS_2, INVALID_EMAIL_ADDRESS);
        when(emailValidationService.validate(EMAIL_ADDRESS_1)).thenReturn(true);
        when(emailValidationService.validate(EMAIL_ADDRESS_2)).thenReturn(true);
        when(emailValidationService.validate(INVALID_EMAIL_ADDRESS)).thenReturn(false);
        when(playerProfileService.findByEmailAddresses(asArray(EMAIL_ADDRESS_1, EMAIL_ADDRESS_2)))
                .thenReturn(Collections.singletonMap(EMAIL_ADDRESS_2, BigDecimal.ZERO));
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getDisplayName()).thenReturn("DISPLAY_NAME");
        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);
        InvitationSendingResult expectedResult = new InvitationSendingResult(1, newHashSet(
                new InvitationSendingResult.Rejection(INVALID_EMAIL_ADDRESS, InvitationSendingResult.ResultCode.INVALID_ADDRESS),
                new InvitationSendingResult.Rejection(EMAIL_ADDRESS_2, InvitationSendingResult.ResultCode.ALREADY_REGISTERED)));

        InvitationSendingResult actualResult =
                underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, emailAddresses, builder, false, USER_IP_ADDRESS);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void challengeFriendsShouldSendEmailsToAllAddresses() {
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getDisplayName()).thenReturn("DISPLAY_NAME");
        ChallengeBuddiesEmailBuilder builder = new ChallengeBuddiesEmailBuilder(REFERRAL_ARRAY, PLAYER_ID, "BLACKJACK", sender);
        when(emailValidationService.validate(anyString())).thenReturn(true);

        underTest.challengeBuddies(asList(EMAIL_ADDRESS_1, EMAIL_ADDRESS_2), builder, PLAYER_ID);

        verify(emailValidationService).validate(EMAIL_ADDRESS_1);
        verify(emailValidationService).validate(EMAIL_ADDRESS_2);

        verify(emailer).quietlySendEmail(builder.withFriendEmailAddress(EMAIL_ADDRESS_1).buildRequest(playerProfileService));
        verify(emailer).quietlySendEmail(builder.withFriendEmailAddress(EMAIL_ADDRESS_2).buildRequest(playerProfileService));
    }

    @Test
    public void challengeFriendsShouldFilterOutNullAddresses() {
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getDisplayName()).thenReturn("DISPLAY_NAME");
        ChallengeBuddiesEmailBuilder builder = new ChallengeBuddiesEmailBuilder(REFERRAL_ARRAY, PLAYER_ID, "BLACKJACK", sender);
        when(emailValidationService.validate(EMAIL_ADDRESS_1)).thenReturn(true);
        when(emailValidationService.validate(EMAIL_ADDRESS_2)).thenReturn(true);
        when(emailValidationService.validate(null)).thenThrow(new NullPointerException("email address may not be null"));

        underTest.challengeBuddies(asList(EMAIL_ADDRESS_1, null, EMAIL_ADDRESS_2), builder, PLAYER_ID);

        verify(emailer).quietlySendEmail(builder.withFriendEmailAddress(EMAIL_ADDRESS_1).buildRequest(playerProfileService));
        verify(emailer).quietlySendEmail(builder.withFriendEmailAddress(EMAIL_ADDRESS_2).buildRequest(playerProfileService));
    }

    @Test
    public void challengeFriendsShouldTrackChallengesWithTracking() {
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getDisplayName()).thenReturn("DISPLAY_NAME");
        ChallengeBuddiesEmailBuilder builder = new ChallengeBuddiesEmailBuilder(REFERRAL_ARRAY, PLAYER_ID, "BLACKJACK", sender);
        when(emailValidationService.validate(anyString())).thenReturn(true);

        underTest.challengeBuddies(asList(EMAIL_ADDRESS_1, EMAIL_ADDRESS_2), builder, PLAYER_ID);

        final Map<String, String> props = newHashMap();
        props.put("challengeType", "email");
        props.put("playersChallenged", "2");
        props.put("challengeSource", "overlay");
        verify(trackingService).trackEvent(null, PLAYER_ID, "sentChallenges", props);

    }

    @Test
    public void shouldCheckWontExceedLimit() {
        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);
        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, asArray(EMAIL_ADDRESS_1), builder, false, USER_IP_ADDRESS);

        verify(limiter).canSendInvitations(eq(1), any(BigDecimal.class), eq(USER_IP_ADDRESS));
    }

    @Test
    public void shouldCheckWithRightNumberOfEmails() {
        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);
        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, asArray(EMAIL_ADDRESS_1, EMAIL_ADDRESS_2), builder, false, USER_IP_ADDRESS);

        verify(limiter).canSendInvitations(eq(2), any(BigDecimal.class), eq(USER_IP_ADDRESS));
    }

    @Test
    public void shouldNotContinueIfLimitExceeded() {
        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);
        when(limiter.canSendInvitations(anyInt(), any(BigDecimal.class), eq(USER_IP_ADDRESS))).thenReturn(false);
        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, asArray(EMAIL_ADDRESS_1, EMAIL_ADDRESS_2), builder, false, USER_IP_ADDRESS);

        verifyZeroInteractions(emailer);
        verifyZeroInteractions(emailValidationService);
        verifyZeroInteractions(playerProfileService);
        verifyZeroInteractions(invitationService);
    }

    @Test
    public void shouldReturnError() {
        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);
        when(limiter.canSendInvitations(anyInt(), any(BigDecimal.class), eq(USER_IP_ADDRESS))).thenReturn(false);

        final InvitationSendingResult expectedResult = new InvitationSendingResult(
                0,
                newHashSet(
                        new InvitationSendingResult.Rejection(EMAIL_ADDRESS_1, InvitationSendingResult.ResultCode.LIMIT_EXCEEDED),
                        new InvitationSendingResult.Rejection(EMAIL_ADDRESS_2, InvitationSendingResult.ResultCode.LIMIT_EXCEEDED)
                )
        );
        final InvitationSendingResult actualResult = underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, asArray(EMAIL_ADDRESS_1, EMAIL_ADDRESS_2), builder, false, USER_IP_ADDRESS);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void shouldTrackSentInvitationsWithLimiter() {
        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);
        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, asArray(EMAIL_ADDRESS_1), builder, false, USER_IP_ADDRESS);

        verify(limiter).canSendInvitations(eq(1), any(BigDecimal.class), eq(USER_IP_ADDRESS));
        verify(limiter).hasSentInvitations(eq(1), any(BigDecimal.class), eq(USER_IP_ADDRESS));
    }


    @Test
    public void shouldCheckLimitWithIpAddress() {
        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);
        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, asArray(EMAIL_ADDRESS_1), builder, false, USER_IP_ADDRESS);

        verify(limiter).canSendInvitations(eq(1), any(BigDecimal.class), eq(USER_IP_ADDRESS));
        verify(limiter).hasSentInvitations(eq(1), any(BigDecimal.class), eq(USER_IP_ADDRESS));
    }

    @Test
    public void shouldCheckLimitWithGivenIpAddress() {
        InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, REFERRAL_URL, PLAYER_ID);
        underTest.emailFriends(PLAYER_ID, GAME_TYPE, SOURCE, asArray(EMAIL_ADDRESS_1), builder, false, "tmp");

        verify(limiter).canSendInvitations(eq(1), any(BigDecimal.class), eq("tmp"));
        verify(limiter).hasSentInvitations(eq(1), any(BigDecimal.class), eq("tmp"));
    }

    private String[] asArray(String... items) {
        return items;
    }

}
