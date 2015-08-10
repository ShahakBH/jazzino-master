package com.yazino.platform.invitation.service;

import com.yazino.platform.invitation.Invitation;
import com.yazino.platform.invitation.InvitationRepository;
import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.invitation.InvitationStatus;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InvitationQueryServiceImplTest {
    private static final BigDecimal SENDER_PLAYER_ID = new BigDecimal(-2784);
    private static final DateTime LAST_UPDATED = new DateTime(2012, 7, 20, 14, 49, 30, 500);
    private static final DateTime CREATED = new DateTime(2012, 4, 20, 14, 59, 20, 700);
    private static final BigDecimal CHIPS_EARNED = BigDecimal.valueOf(7000);

    @Mock
    private InvitationRepository invitationRepository;

    private InvitationQueryServiceImpl underTest;

    @Before
    public void setup() {
        underTest = new InvitationQueryServiceImpl(invitationRepository);
    }

    @Test
    public void findInvitationsByIssuingPlayer_shouldLookupInvitationsUsingInvitationRepository() {
        // invitations in repository
        com.yazino.platform.invitation.persistence.Invitation repositoryInvitation =
                new com.yazino.platform.invitation.persistence.Invitation(
                        SENDER_PLAYER_ID, "recipientIdentifier_1", InvitationSource.EMAIL, InvitationStatus.ACCEPTED,
                        CHIPS_EARNED.longValue(), CREATED, LAST_UPDATED, "SLOTS", "SOURCE");
        Set<com.yazino.platform.invitation.persistence.Invitation> repositoryInvitations =
                new HashSet<com.yazino.platform.invitation.persistence.Invitation>(asList(repositoryInvitation));
        when(invitationRepository.getInvitationsSentBy(SENDER_PLAYER_ID)).thenReturn(repositoryInvitations);

        // invitations to be returned by service
        Invitation invitation = new Invitation(
                SENDER_PLAYER_ID, "recipientIdentifier_1", InvitationSource.EMAIL, InvitationStatus.ACCEPTED,
                CREATED, LAST_UPDATED, CHIPS_EARNED);
        Set<Invitation> expectedInvitations = new HashSet<Invitation>(asList(invitation));

        Set<Invitation> actualInvitations = underTest.findInvitationsByIssuingPlayer(SENDER_PLAYER_ID);

        assertEquals(expectedInvitations, actualInvitations);
    }


}
