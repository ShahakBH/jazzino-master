package com.yazino.platform.invitation.service;

import com.yazino.email.EmailException;
import com.yazino.platform.event.message.PlayerReferrerEvent;
import com.yazino.platform.invitation.InvitationRepository;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.invitation.InvitationStatus;
import com.yazino.platform.invitation.persistence.Invitation;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static com.yazino.platform.invitation.InvitationStatus.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class InvitationTrackingServiceTest {

    private static final BigDecimal ISSUING_PLAYER_ID = BigDecimal.valueOf(100L);
    private static final BigDecimal ISSUING_PLAYER_ID_2 = BigDecimal.valueOf(200L);
    private static final BigDecimal ISSUING_PLAYER_ID_3 = BigDecimal.valueOf(300L);
    private static final BigDecimal RECIPIENT_PLAYER_ID = BigDecimal.valueOf(400L);
    private static final String RECIPIENT_IDENTIFIER = "auser@example.com";
    private static final InvitationSource RECIPIENT_SOURCE = InvitationSource.EMAIL;
    private static final String GAME_TYPE = "GAME_TYPE";
    private static final String SCREEN_SOURCE = "FB_SCREEN";
    private static final DateTime CREATED_TIME = new DateTime(2011, 4, 5, 12, 12, 12, 120);
    private static final DateTime UPDATED_TIME = new DateTime(2011, 4, 6, 12, 12, 12, 120);
    private static final DateTime REGISTERED_TIME = new DateTime(2011, 6, 7, 8, 9, 10, 110);
    private static final long REWARD_AMOUNT = 5000l;
    private static final InvitationSource SOURCE = InvitationSource.FACEBOOK;

    private InvitationRepository invitationRepository = mock(InvitationRepository.class);

    private ReferredFriendsService referredFriendsService = mock(ReferredFriendsService.class);

    @Mock
    private QueuePublishingService<PlayerReferrerEvent> playerReferrerEventService;

    private InvitationTrackingService underTest;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new InvitationTrackingService(
                invitationRepository,
                referredFriendsService,
                playerReferrerEventService);
    }

    @Test
    public void invitationSent_shouldSaveInvitationInRepository() {
        Invitation expectedInvitation = new Invitation(
                ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, SOURCE, WAITING,
                CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);

        underTest.invitationSent
                (ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, SOURCE, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);

        verify(invitationRepository).save(expectedInvitation);
    }

    @Test
    public void invitationSent_shouldChangeStatusToWaitingRemindedIfWaiting() {
        when(invitationRepository.getInvitationByIssuingPlayerRecipientAndSource(ISSUING_PLAYER_ID,
                RECIPIENT_IDENTIFIER, SOURCE)).thenReturn(new Invitation(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER,
                SOURCE, WAITING, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE));

        Invitation expectedInvitation = new Invitation(
                ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, SOURCE, WAITING_REMINDED,
                CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);

        underTest.invitationSent
                (ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, SOURCE, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);

        verify(invitationRepository).save(expectedInvitation);
    }

    @Test
    public void invitationSent_shouldUpdateTimeWhenChangingStatusToWaitingReminded() {
        when(invitationRepository.getInvitationByIssuingPlayerRecipientAndSource(ISSUING_PLAYER_ID,
                RECIPIENT_IDENTIFIER, SOURCE)).thenReturn(new Invitation(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER,
                SOURCE, WAITING, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE));

        Invitation expectedInvitation = new Invitation(
                ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, SOURCE, WAITING_REMINDED, null,
                CREATED_TIME, UPDATED_TIME, GAME_TYPE, SCREEN_SOURCE);

        underTest.invitationSent
                (ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, SOURCE, UPDATED_TIME, GAME_TYPE, SCREEN_SOURCE);

        verify(invitationRepository).save(expectedInvitation);
    }

    @Test
    public void invitationSent_shouldIgnoreIfReminderAlreadySent() {
        when(invitationRepository.getInvitationByIssuingPlayerRecipientAndSource(ISSUING_PLAYER_ID,
                RECIPIENT_IDENTIFIER, SOURCE)).thenReturn(new Invitation(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER,
                SOURCE, WAITING_REMINDED, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE));

        underTest.invitationSent
                (ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, SOURCE, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);

        verify(invitationRepository, never()).save(any(Invitation.class));
    }


    @Test
    public void invitationSent_shouldIgnoreIfAlreadyAccepted() {
        when(invitationRepository.getInvitationByIssuingPlayerRecipientAndSource(ISSUING_PLAYER_ID,
                RECIPIENT_IDENTIFIER, SOURCE)).thenReturn(new Invitation(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER,
                SOURCE, ACCEPTED, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE));

        underTest.invitationSent
                (ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, SOURCE, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);

        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    public void invitationSent_shouldIgnoreIfAlreadyAcceptedOther() {
        when(invitationRepository.getInvitationByIssuingPlayerRecipientAndSource(ISSUING_PLAYER_ID,
                RECIPIENT_IDENTIFIER, SOURCE)).thenReturn(new Invitation(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER,
                SOURCE, ACCEPTED_OTHER, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE));

        underTest.invitationSent
                (ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, SOURCE, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);

        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    public void invitationSent_shouldIgnoreIfAlreadyRejected() {
        when(invitationRepository.getInvitationByIssuingPlayerRecipientAndSource(ISSUING_PLAYER_ID,
                RECIPIENT_IDENTIFIER, SOURCE)).thenReturn(new Invitation(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER,
                SOURCE, NOT_ACCEPTED, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE));

        underTest.invitationSent
                (ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, SOURCE, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);

        verify(invitationRepository, never()).save(any(Invitation.class));
    }

    @Test
    public void invitationAccepted_shouldChangeAllStatusesToAcceptedIfWaitingOrWaitingReminded() {
        Invitation invitation1 = new Invitation(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, InvitationStatus.WAITING, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);
        Invitation invitationUpdated1 = new Invitation(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, InvitationStatus.ACCEPTED, REWARD_AMOUNT, CREATED_TIME, REGISTERED_TIME, GAME_TYPE, SCREEN_SOURCE);

        Invitation invitation2 = new Invitation(ISSUING_PLAYER_ID_2, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, InvitationStatus.WAITING, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);
        Invitation invitationUpdated2 = new Invitation(ISSUING_PLAYER_ID_2, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, InvitationStatus.ACCEPTED, REWARD_AMOUNT, CREATED_TIME, REGISTERED_TIME, GAME_TYPE, SCREEN_SOURCE);

        Invitation invitation3 = new Invitation(ISSUING_PLAYER_ID_3, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, InvitationStatus.WAITING_REMINDED, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);
        Invitation invitationUpdated3 = new Invitation(ISSUING_PLAYER_ID_3, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, InvitationStatus.ACCEPTED, REWARD_AMOUNT, CREATED_TIME, REGISTERED_TIME, GAME_TYPE, SCREEN_SOURCE);

        Collection<Invitation> invitations = Arrays.asList(invitation1, invitation2, invitation3);
        when(invitationRepository.getInvitationsSentTo(RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE)).thenReturn(invitations);
        when(referredFriendsService.processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID)).thenReturn(new BigDecimal(REWARD_AMOUNT));
        when(referredFriendsService.processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID_2)).thenReturn(new BigDecimal(REWARD_AMOUNT));
        when(referredFriendsService.processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID_3)).thenReturn(new BigDecimal(REWARD_AMOUNT));

        underTest.invitationAccepted(RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, REGISTERED_TIME, RECIPIENT_PLAYER_ID);

        verify(invitationRepository).save(invitationUpdated1);
        verify(invitationRepository).save(invitationUpdated2);
        verify(invitationRepository).save(invitationUpdated3);
    }

    @Test
    public void invitationAccepted_shouldProcessReferrals() throws EmailException {
        Invitation invitation1 = new Invitation(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, InvitationStatus.WAITING, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);
        Invitation invitation2 = new Invitation(ISSUING_PLAYER_ID_2, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, InvitationStatus.WAITING, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);
        Invitation invitation3 = new Invitation(ISSUING_PLAYER_ID_3, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, InvitationStatus.WAITING_REMINDED, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);

        Collection<Invitation> invitations = Arrays.asList(invitation1, invitation2, invitation3);
        when(invitationRepository.getInvitationsSentTo(RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE)).thenReturn(invitations);
        when(referredFriendsService.processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID)).thenReturn(new BigDecimal(REWARD_AMOUNT));
        when(referredFriendsService.processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID_2)).thenReturn(new BigDecimal(REWARD_AMOUNT));
        when(referredFriendsService.processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID_3)).thenReturn(new BigDecimal(REWARD_AMOUNT));

        underTest.invitationAccepted(RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, REGISTERED_TIME, RECIPIENT_PLAYER_ID);

        verify(referredFriendsService).processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID);
        assertEquals((Long) REWARD_AMOUNT, invitation1.getRewardAmount());
        verify(referredFriendsService).processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID_2);
        assertEquals((Long) REWARD_AMOUNT, invitation2.getRewardAmount());
        verify(referredFriendsService).processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID_3);
        assertEquals((Long) REWARD_AMOUNT, invitation3.getRewardAmount());
    }

    @Test
    public void acceptedInvitationShouldSpawnReferralMessage(){
        Invitation invitation1 = new Invitation(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, InvitationStatus.WAITING, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);
        when(invitationRepository.getInvitationsSentTo(RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE)).thenReturn(newArrayList(invitation1));

        when(referredFriendsService.processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID)).thenReturn(new BigDecimal(REWARD_AMOUNT));
        when(referredFriendsService.processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID_2)).thenReturn(new BigDecimal(REWARD_AMOUNT));
        when(referredFriendsService.processReferral(RECIPIENT_PLAYER_ID, ISSUING_PLAYER_ID_3)).thenReturn(new BigDecimal(REWARD_AMOUNT));

        underTest.invitationAccepted(RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, REGISTERED_TIME, RECIPIENT_PLAYER_ID);

        verify(playerReferrerEventService).send(new PlayerReferrerEvent(RECIPIENT_PLAYER_ID, "INVITE",null,null));
    }
}
