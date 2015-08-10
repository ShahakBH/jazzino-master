package com.yazino.web.service;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.game.api.GameType;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.web.data.GameTypeRepository;
import com.yazino.web.domain.ApplicationInformation;
import com.yazino.web.domain.email.ChallengeBuddiesEmailBuilder;
import com.yazino.web.domain.email.InviteFriendsEmailDetails;
import com.yazino.web.domain.email.ToFriendEmailBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.platform.Partner.YAZINO;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InvitationLobbyServiceTest {
    private static final String MESSAGE = "message";
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final String USER_IP_ADDRESS = "192.168.1.1";
    private static final String SENDER = "from@your.mum";
    private static final ApplicationInformation APP_INFO = new ApplicationInformation("gameType", YAZINO, Platform.ANDROID);
    private static final String YAZINO_URL = "https://www.yazino.com:80/";
    private static final String REF_PARAM = "?ref=email_reflex_invite";

    @Mock
    private InvitationLobbyEmailService invitationLobbyEmailService;
    @Mock
    private InvitationLobbyFacebookService invitationLobbyFacebookService;
    @Mock
    private GameTypeRepository gameTypeRepository;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private final Map<String, GameTypeInformation> gameTypes = new HashMap<>();

    private InvitationLobbyService underTest;

    @Before
    public void setUp() throws Exception {
        when(gameTypeRepository.getGameTypes()).thenReturn(gameTypes);
        when(yazinoConfiguration.getString("strata.email.from-address")).thenReturn(SENDER);
        when(yazinoConfiguration.getString("strata.referral.param")).thenReturn(REF_PARAM);
        when(yazinoConfiguration.getString("strata.public.url")).thenReturn(YAZINO_URL);

        final HashSet<String> objects = new HashSet<>();
        objects.add("wheelDeal");
        gameTypes.put("SLOTS", new GameTypeInformation(new GameType("SLOTS", "Wheel Deal", objects), true));

        final PlayerProfileService playerProfileService = mock(PlayerProfileService.class);

        underTest = new InvitationLobbyService(invitationLobbyEmailService, invitationLobbyFacebookService,
                playerProfileService, yazinoConfiguration);
    }

    @Test
    public void sendInvitations_shouldSendEmailsWithDefaultCtaWhereNoPropertyIsPresent() {
        underTest.sendInvitations(PLAYER_ID, APP_INFO, "source", MESSAGE, new String[]{"email@example.com"}, false, USER_IP_ADDRESS);

        final InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, YAZINO_URL + "gameType?ref=email_reflex_invite", PLAYER_ID);
        verify(invitationLobbyEmailService).emailFriends(PLAYER_ID, "gameType", "source", new String[]{"email@example.com"},
                builder, false, USER_IP_ADDRESS);
    }

    @Test
    public void sendInvitations_shouldSendEmailsWithCtaFromPropertyWherePresent() {
        when(yazinoConfiguration.getString("invitation.email.cta.YAZINO.ANDROID.gameType", null)).thenReturn("aPropertyCta");

        underTest.sendInvitations(PLAYER_ID, APP_INFO, "source", MESSAGE, new String[]{"email@example.com"}, false, USER_IP_ADDRESS);

        final InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(MESSAGE, "aPropertyCta", PLAYER_ID);
        verify(invitationLobbyEmailService).emailFriends(PLAYER_ID, "gameType", "source", new String[]{"email@example.com"},
                builder, false, USER_IP_ADDRESS);
    }

    @Test
    public void sendInvitations_shouldReturnSendingResult() {
        InvitationSendingResult expectedResult = new InvitationSendingResult(3, Collections.<InvitationSendingResult.Rejection>emptySet());
        when(invitationLobbyEmailService.emailFriends(any(BigDecimal.class), anyString(), anyString(),
                any(String[].class), any(InviteFriendsEmailDetails.class), anyBoolean(), eq(USER_IP_ADDRESS))).thenReturn(expectedResult);

        InvitationSendingResult actualResult = underTest.sendInvitations(
                PLAYER_ID, APP_INFO, "source", MESSAGE, new String[]{"email@example.com"}, false, USER_IP_ADDRESS);

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void shouldSendReminderEmail() {
        underTest.sendInvitationReminders(PLAYER_ID, new String[]{"email@example.com"}, "gameType", USER_IP_ADDRESS);
        final InviteFriendsEmailDetails builder = new InviteFriendsEmailDetails(null, YAZINO_URL, PLAYER_ID);
        verify(invitationLobbyEmailService).emailFriends(PLAYER_ID, "gameType", "INVITATION_STATEMENT", new String[]{"email@example.com"}, builder, false, USER_IP_ADDRESS);
    }

    @Test
    public void shouldTrackFacebookInvites() {
        underTest.trackFacebookInvites(PLAYER_ID, "gameType", "source", "r1", "r2");
        verify(invitationLobbyFacebookService).trackInvitesSent(PLAYER_ID, "gameType", "source", "r1", "r2");
    }

    @Test
    public void challengeShouldSendEmail() {
        Map<String, GameTypeInformation> gameTypes = newHashMap();
        GameTypeInformation gameTypeInfo = mock(GameTypeInformation.class);
        GameType gameType = mock(GameType.class);
        when(gameTypeRepository.getGameTypes()).thenReturn(gameTypes);
        when(gameTypeInfo.getGameType()).thenReturn(gameType);
        final HashSet<String> strings = new HashSet<>();
        strings.add("wheelDeal");
        when(gameType.getPseudonyms()).thenReturn(strings);
        gameTypes.put("SLOTS", gameTypeInfo);

        underTest.challengeBuddiesWithEmails(PLAYER_ID, "SLOTS", newArrayList("email@example.com"));
        final ToFriendEmailBuilder builder = new ChallengeBuddiesEmailBuilder(new String[]{"https://www.yazino.com:80/SLOTS?ref=", "email_reflex_slots_challenge"}, PLAYER_ID, "SLOTS",
                SENDER);
        verify(invitationLobbyEmailService).challengeBuddies(newArrayList("email@example.com"), builder, PLAYER_ID);
    }

    @Test
    public void buildReferralUrlShouldModifyUrlForChallengeUse() {
        assertThat(underTest.buildReferralUrl(YAZINO_URL + REF_PARAM, "SLOTS"), is(equalTo(new String[]{"https://www.yazino.com:80/SLOTS?ref=", "email_reflex_slots_challenge"})));
    }
}
