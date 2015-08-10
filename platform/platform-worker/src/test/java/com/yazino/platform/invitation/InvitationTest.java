package com.yazino.platform.invitation;

import com.yazino.platform.event.message.InvitationEvent;
import com.yazino.platform.invitation.persistence.Invitation;
import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class InvitationTest {

    private static final BigDecimal ISSUING_PLAYER_ID = BigDecimal.valueOf(100L);
    private static final String RECIPIENT_IDENTIFIER = "auser@example.com";
    private static final InvitationSource RECIPIENT_SOURCE = InvitationSource.EMAIL;
    private static final String GAME_TYPE = "GAME_TYPE";
    private static final String SCREEN_SOURCE = "FB_SCREEN";
    public static final long REWARD_AMOUNT = 5000l;
    private static final InvitationStatus STATUS = InvitationStatus.WAITING;
    private static final DateTime CREATED_TIME = new DateTime(2011, 4, 5, 12, 12, 12, 120);
    private static final DateTime REGISTERED_TIME = new DateTime(2011, 6, 7, 8, 9, 10, 110);

    @Test
    public void shouldConvertToEvent_NewlyCreatedInvite() {
        Invitation invitation = new Invitation(ISSUING_PLAYER_ID,
                RECIPIENT_IDENTIFIER,
                RECIPIENT_SOURCE,
                STATUS,
                CREATED_TIME,
                GAME_TYPE,
                SCREEN_SOURCE);
        InvitationEvent expectedEvent = new InvitationEvent(ISSUING_PLAYER_ID,
                RECIPIENT_IDENTIFIER,
                RECIPIENT_SOURCE,
                STATUS.name(),
                null,
                GAME_TYPE,
                SCREEN_SOURCE,
                CREATED_TIME,
                CREATED_TIME);
        final InvitationEvent invitationEvent = invitation.toEvent();
        assertEquals(expectedEvent, invitationEvent);
    }

    @Test
    public void shouldConvertToEvent_UpdatedInvite() {
        Invitation invitation = new Invitation(ISSUING_PLAYER_ID,
                RECIPIENT_IDENTIFIER,
                RECIPIENT_SOURCE,
                STATUS,
                REWARD_AMOUNT,
                CREATED_TIME,
                REGISTERED_TIME,
                GAME_TYPE,
                SCREEN_SOURCE);
        InvitationEvent expectedEvent = new InvitationEvent(ISSUING_PLAYER_ID,
                RECIPIENT_IDENTIFIER,
                RECIPIENT_SOURCE,
                STATUS.name(),
                new BigDecimal(REWARD_AMOUNT),
                GAME_TYPE,
                SCREEN_SOURCE,
                CREATED_TIME,
                REGISTERED_TIME);
        final InvitationEvent invitationEvent = invitation.toEvent();
        assertEquals(expectedEvent, invitationEvent);
    }
}
