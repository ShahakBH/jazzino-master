package com.yazino.web.session;

import com.yazino.email.EmailException;
import com.yazino.email.EmailService;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.community.*;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.Session;
import com.yazino.platform.session.SessionService;
import com.yazino.web.data.CurrencyRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;

import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.Platform.WEB;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SessionRegistrarTest {
    private static final BigDecimal PLAYER_ID = new BigDecimal(100);
    private static final BigDecimal SESSION_ID = new BigDecimal(134);
    private static final BigDecimal REFERRAL_PLAYER_ID = new BigDecimal(300);
    private static final BigDecimal GUEST_ACCOUNT_CONVERSION_AMOUNT = new BigDecimal(9876);
    public static final HashMap<String,Object> CLIENT_CONTEXT_MAP = new HashMap<>();

    @Mock
    private CommunityService communityService;
    @Mock
    private EmailService emailService;
    @Mock
    private PlayerProfileService playerProfileService;
    @Mock
    private SessionService sessionService;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private PlayerService playerService;

    private BigDecimal playerId = new BigDecimal("11.11");
    private BigDecimal accountId = new BigDecimal("11.12");
    private String pictureUrl = "http://url";
    private PlayerCreditConfiguration creditConfig = new PlayerCreditConfiguration(new BigDecimal(0),
            new BigDecimal(1), GUEST_ACCOUNT_CONVERSION_AMOUNT);

    private SessionRegistrar underTest;

    @Before
    public void wireMocks() {
        underTest = new SessionRegistrar(communityService, currencyRepository,
                playerProfileService, playerService, sessionService, creditConfig);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testShouldRegisterNewSessionAndSendInvitationConfirmation() throws EmailException {
        // GIVEN the user registration info
        final PartnerSession partnerSession = new PartnerSession(null, null, null, null, null);
        final Session session = new Session(SESSION_ID, PLAYER_ID, YAZINO, Platform.WEB, "ipAddress", "aLocalSessionkey", "aNickname", "anEmail",
                "aPictureUrl", BigDecimal.ZERO, new DateTime(), Collections.<Location>emptySet(), Collections.<String>emptySet());

        final PlayerProfile userProfile = new PlayerProfile();
        userProfile.setPlayerId(PLAYER_ID);
        userProfile.setReferralIdentifier(REFERRAL_PLAYER_ID.toPlainString());
        userProfile.setProviderName("FACEBOOK");
        userProfile.setExternalId("1000");
        userProfile.setFirstName("Machin");
        userProfile.setLastName("Bidule");
        userProfile.setRpxProvider("YAZRPX");

        when(playerService.createNewPlayer(anyString(),
                anyString(), any(GuestStatus.class), any(PaymentPreferences.class), any(PlayerCreditConfiguration.class)))
                .thenReturn(new BasicProfileInformation(PLAYER_ID, "Machin Bidule", pictureUrl, accountId));
        given(playerProfileService.findByPlayerId(PLAYER_ID)).willReturn(userProfile);
        given(sessionService.createSession(any(BasicProfileInformation.class), any(Partner.class),
                anyString(), anyString(), anyString(), any(Platform.class), anyString(), anyMap())).willReturn(session);
        given(playerService.getBasicProfileInformation(PLAYER_ID)).willReturn(
                new BasicProfileInformation(PLAYER_ID, "aPlayer", "aPictureUrl", new BigDecimal(10)));
        given(playerService.getBasicProfileInformation(REFERRAL_PLAYER_ID)).willReturn(
                new BasicProfileInformation(REFERRAL_PLAYER_ID, "Machin", "aPictureUrl", new BigDecimal(30)));

        final PlayerProfile referrerProfile = new PlayerProfile();
        referrerProfile.setReferralIdentifier("10");
        referrerProfile.setProviderName("YAZINO");
        referrerProfile.setExternalId("100");
        referrerProfile.setRpxProvider("YAZRPX");
        given(playerProfileService.findByPlayerId(REFERRAL_PLAYER_ID)).willReturn(referrerProfile);

        // WHEN asking the service to register the new session
        underTest.registerNewSession(PLAYER_ID, partnerSession, WEB, LoginResult.NEW_USER, CLIENT_CONTEXT_MAP);

    }

    @Test
    public void convertToLobbySession_stores_session_info() {
        final Partner partnerId = YAZINO;
        final String playerName = "pera";
        final String localSessionKey = "SKSKSKS";
        final String playerEmail = "email";
        final Session ps = new Session(SESSION_ID, playerId, partnerId, Platform.WEB, "ipAddress",
                localSessionKey, playerName, playerEmail, pictureUrl,
                BigDecimal.ZERO, new DateTime(), Collections.<Location>emptySet(), Collections.<String>emptySet());
        assertEquals(new LobbySession(SESSION_ID, playerId, playerName, localSessionKey, partnerId, null, playerEmail,
                null, true, WEB, AuthProvider.YAZINO), LobbySession.forSession(ps, true, WEB, AuthProvider.YAZINO));
    }

}
