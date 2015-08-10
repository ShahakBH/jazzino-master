package com.yazino.platform.invitation.service;

import com.google.common.collect.Sets;
import com.yazino.email.EmailException;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerCreditConfiguration;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.invitation.emailService.AcceptedInviteFriendsEmailService;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReferredFriendsServiceTest {

    private static final PlayerProfile SENDER_PROFILE = new PlayerProfile();
    private static final PlayerProfile RECEIVER_PROFILE = new PlayerProfile();
    private static final BigDecimal RECEIVER_PLAYER_ID = new BigDecimal(-2785);
    private static final int AWARD_AMOUNT = 12345;
    private static final BigDecimal SENDER_PLAYER_ID = new BigDecimal(-2784);
    private static final String DISPLAY_NAME = "Charazard";
    private static final String EMAIL_ADDRESS = "Charazard@gotocatchthemall.com";
    private static final String SENDER_FIRST_NAME = "Desiree";

    @Mock
    private AcceptedInviteFriendsEmailService acceptedInviteFriendsEmailService;

    @Mock
    private PlayerService playerService;

    @Mock
    private PlayerCreditConfiguration playerCreditConfiguration;

    @Mock
    private PlayerProfileService playerProfileService;
    private ReferredFriendsService underTest;

    @Before
    public void setup() {
        SENDER_PROFILE.setEmailAddress(EMAIL_ADDRESS);
        SENDER_PROFILE.setDisplayName(DISPLAY_NAME);
        RECEIVER_PROFILE.setFirstName(SENDER_FIRST_NAME);
        when(playerProfileService.findByPlayerId(SENDER_PLAYER_ID)).thenReturn(SENDER_PROFILE);
        when(playerProfileService.findByPlayerId(RECEIVER_PLAYER_ID)).thenReturn(RECEIVER_PROFILE);
        final String fromAddress = "from@your.mum";
        underTest = new ReferredFriendsService(
                acceptedInviteFriendsEmailService,
                playerService,
                playerCreditConfiguration,
                playerProfileService, fromAddress);
    }

    @Test
    public void processReferralShouldEmailReferrerAcceptanceEmail() throws EmailException {
        underTest.processReferral(RECEIVER_PLAYER_ID, SENDER_PLAYER_ID);
        verify(acceptedInviteFriendsEmailService)
                .sendInviteFriendsAcceptedEmail(DISPLAY_NAME, EMAIL_ADDRESS, SENDER_FIRST_NAME, "Charazard <from@your.mum>");
    }

    @Test
    public void processReferralShouldEmailReferrerAcceptanceEmailWithFormattedSender() throws EmailException {
        underTest = new ReferredFriendsService(
                acceptedInviteFriendsEmailService,
                playerService,
                playerCreditConfiguration,
                playerProfileService, "dis <isfrom@your.mum>");
        underTest.processReferral(RECEIVER_PLAYER_ID, SENDER_PLAYER_ID);
        verify(acceptedInviteFriendsEmailService)
                .sendInviteFriendsAcceptedEmail(DISPLAY_NAME, EMAIL_ADDRESS, SENDER_FIRST_NAME, "Charazard <isfrom@your.mum>");
    }

    @Test
    public void processReferralShouldOnlyEmailReferrerAcceptanceEmailIfTheProfileContainsAnEmailAddress() throws EmailException {
        reset(playerProfileService);
        SENDER_PROFILE.setEmailAddress(null);
        when(playerProfileService.findByPlayerId(SENDER_PLAYER_ID)).thenReturn(SENDER_PROFILE);
        when(playerProfileService.findByPlayerId(RECEIVER_PLAYER_ID)).thenReturn(RECEIVER_PROFILE);

        underTest.processReferral(RECEIVER_PLAYER_ID, SENDER_PLAYER_ID);
        verifyZeroInteractions(acceptedInviteFriendsEmailService);
    }

    @Test
    public void processAwardChipsToSender() throws EmailException, WalletServiceException {
        when(playerCreditConfiguration.getReferralAmount()).thenReturn(new BigDecimal(AWARD_AMOUNT));
        underTest.processReferral(RECEIVER_PLAYER_ID, SENDER_PLAYER_ID);
        verify(playerService).postTransaction(SENDER_PLAYER_ID, null, new BigDecimal(AWARD_AMOUNT), "Referral",
                "Player with id -2785 accepted invite");
    }

    @Test
    public void shouldAwardZeroChipsWhenExceptionThrown() throws WalletServiceException {
        doThrow(new WalletServiceException("message")).
                when(playerService).postTransaction(any(BigDecimal.class), (BigDecimal) isNull(), any(BigDecimal.class), anyString(),
                anyString());
        assertEquals(BigDecimal.ZERO, underTest.processReferral(RECEIVER_PLAYER_ID, SENDER_PLAYER_ID));
    }

    @Test
    public void shouldSetBothPlayersToBeBuddiesOfEachOther() {

        underTest.processReferral(RECEIVER_PLAYER_ID, SENDER_PLAYER_ID);
        verify(playerService).registerFriends(RECEIVER_PLAYER_ID, Sets.newHashSet(SENDER_PLAYER_ID));
        verify(playerService).registerFriends(SENDER_PLAYER_ID, Sets.newHashSet(RECEIVER_PLAYER_ID));

    }
}
