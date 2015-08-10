package com.yazino.platform.invitation;

import com.yazino.platform.event.message.InvitationEvent;
import com.yazino.platform.invitation.persistence.*;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class InvitationRepositoryTest {

    private static final DateTime TIME = new DateTime(2009, 3, 30, 23, 45, 59, 0);
    public static final BigDecimal ISSUING_PLAYER_ID = BigDecimal.ZERO;
    public static final BigDecimal ISSUING_PLAYER_ID_2 = BigDecimal.ONE;
    public static final BigDecimal ISSUING_PLAYER_ID_3 = BigDecimal.TEN;
    public static final String RECIPIENT_IDENTIFIER = "recipient";

    private InvitationRepository invitationRepository;
    private JDBCInvitationDAO dao;
    private QueuePublishingService<InvitationEvent> invitationEventPublisher;

    @Before
    public void setUp() throws Exception {
        dao = mock(JDBCInvitationDAO.class);
        invitationEventPublisher = mock(QueuePublishingService.class);
        invitationRepository = new InvitationRepository(dao, invitationEventPublisher);
    }

    @Test
    public void shouldAllowingSave() {
        final com.yazino.platform.invitation.persistence.Invitation invitation = new com.yazino.platform.invitation.persistence.Invitation(ISSUING_PLAYER_ID, "recipient", InvitationSource.EMAIL,
                InvitationStatus.WAITING, TIME, "gameType", "screen");
        invitationRepository.save(invitation);
        verify(dao).save(invitation);
    }

    @Test
    public void shouldPublishEventOnSave() {
        final com.yazino.platform.invitation.persistence.Invitation invitation = new com.yazino.platform.invitation.persistence.Invitation(BigDecimal.ONE, "recipient", InvitationSource.EMAIL,
                InvitationStatus.WAITING, TIME, "gameType", "screen");
        invitationRepository.save(invitation);
        final InvitationEvent message = new InvitationEvent(BigDecimal.ONE, "recipient", InvitationSource.EMAIL,
                InvitationStatus.WAITING.toString(), null, "gameType", "screen", TIME, TIME);
        verify(invitationEventPublisher).send(message);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotSaveNullInvitation() {
        invitationRepository.save(null);
    }

    @Test
    public void shouldAllowRetrievingByRecipientAndSource() {
        final com.yazino.platform.invitation.persistence.Invitation invitation1 = new com.yazino.platform.invitation.persistence.Invitation(ISSUING_PLAYER_ID, "recipient", InvitationSource.EMAIL,
                InvitationStatus.WAITING, TIME, "gameType", "screen");
        final com.yazino.platform.invitation.persistence.Invitation invitation2 = new com.yazino.platform.invitation.persistence.Invitation(ISSUING_PLAYER_ID_2, "recipient", InvitationSource.EMAIL,
                InvitationStatus.WAITING, TIME, "gameType", "screen");
        final com.yazino.platform.invitation.persistence.Invitation invitation3 = new com.yazino.platform.invitation.persistence.Invitation(ISSUING_PLAYER_ID_3, "recipient", InvitationSource.EMAIL,
                InvitationStatus.WAITING, TIME, "gameType", "screen");
        final List<com.yazino.platform.invitation.persistence.Invitation> expected = Arrays.asList(invitation1, invitation2, invitation3);
        when(dao.getInvitations("recipient", InvitationSource.EMAIL)).thenReturn(expected);
        final Collection<com.yazino.platform.invitation.persistence.Invitation> actual = invitationRepository.getInvitationsSentTo("recipient",
                InvitationSource.EMAIL);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotRetrieveForNullRecipient() {
        invitationRepository.getInvitationsSentTo(null, InvitationSource.FACEBOOK);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotRetrieveForNullSource() {
        invitationRepository.getInvitationsSentTo("recipient", null);
    }

    @Test
    public void getInvitationsBySender_shouldDelegateToDao() {
        final com.yazino.platform.invitation.persistence.Invitation invitation1 = new com.yazino.platform.invitation.persistence.Invitation(ISSUING_PLAYER_ID, "recipient1", InvitationSource.EMAIL,
                InvitationStatus.WAITING, TIME, "gameType", "screen");
        final com.yazino.platform.invitation.persistence.Invitation invitation2 = new com.yazino.platform.invitation.persistence.Invitation(ISSUING_PLAYER_ID, "recipient2", InvitationSource.EMAIL,
                InvitationStatus.WAITING, TIME, "gameType", "screen");
        final List<com.yazino.platform.invitation.persistence.Invitation> expected = Arrays.asList(invitation1, invitation2);
        when(dao.findInvitationsByIssuingPlayerId(ISSUING_PLAYER_ID)).thenReturn(expected);

        final Collection<com.yazino.platform.invitation.persistence.Invitation> actual = invitationRepository.getInvitationsSentBy(ISSUING_PLAYER_ID);

        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void getInvitationsBySender_shouldNotRetrieveForNullIssuingPlayer() {
        invitationRepository.getInvitationsSentBy(null);
    }

    @Test(expected = NullPointerException.class)
    public void getInvitationByIssuingPlayerRecipientAndSource_shouldNotRetrieveForNullIssuingPlayer() {
        invitationRepository.getInvitationByIssuingPlayerRecipientAndSource(null, RECIPIENT_IDENTIFIER,
                InvitationSource.FACEBOOK);
    }

    @Test(expected = NullPointerException.class)
    public void getInvitationByIssuingPlayerRecipientAndSource_shouldNotRetrieveForNullRecipient() {
        invitationRepository.getInvitationByIssuingPlayerRecipientAndSource(ISSUING_PLAYER_ID, null,
                InvitationSource.FACEBOOK);
    }

    @Test(expected = NullPointerException.class)
    public void getInvitationByIssuingPlayerRecipientAndSource_shouldNotRetrieveForNullSource() {
        invitationRepository.getInvitationByIssuingPlayerRecipientAndSource(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, null);
    }

    @Test
    public void InvitationByIssuingPlayerRecipientAndSource_shouldDelegateToDao() {
        final com.yazino.platform.invitation.persistence.Invitation expected = new com.yazino.platform.invitation.persistence.Invitation(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, InvitationSource.EMAIL,
                InvitationStatus.WAITING, TIME, "gameType", "screen");
        when(dao.findInvitationsByIssuingPlayerRecipientAndSource(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, InvitationSource
                .EMAIL)).thenReturn(expected);

        final com.yazino.platform.invitation.persistence.Invitation actual = invitationRepository.getInvitationByIssuingPlayerRecipientAndSource
                (ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, InvitationSource.EMAIL);

        assertEquals(expected, actual);
    }


}
