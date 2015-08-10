package com.yazino.platform.invitation;

import com.yazino.platform.invitation.message.InvitationAcceptedMessage;
import com.yazino.platform.invitation.message.InvitationSentMessage;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class QueuePublishingInvitationServiceTest {
    private static final BigDecimal ISSUING_PLAYER_ID = BigDecimal.valueOf(100L);
    private static final BigDecimal RECIPIENT_PLAYER_ID = BigDecimal.valueOf(101L);
    private static final String RECIPIENT_IDENTIFIER = "auser@somewhere.com";
    private static final InvitationSource RECIPIENT_SOURCE = InvitationSource.EMAIL;
    private static final String GAME_TYPE = "GAME_TYPE";
    private static final String SCREEN_SOURCE = "FB_SCREEN";
    private static final DateTime CREATED_TIME = new DateTime(2011, 2, 13, 15, 56, 30, 789);

    private static final DateTime REGISTRATION_TIME = new DateTime(2001, 12, 13, 11, 56, 30, 789);

    @Mock
    private QueuePublishingService queuePublishingService;
    private QueuePublishingInvitationService underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new QueuePublishingInvitationService(queuePublishingService);
    }

    @Test
    public void shouldPublishInvitationSent() {
        underTest.invitationSent(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, CREATED_TIME, GAME_TYPE,
                SCREEN_SOURCE);

        verify(queuePublishingService).send(
                new InvitationSentMessage(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, CREATED_TIME,
                        GAME_TYPE, SCREEN_SOURCE));
    }
    @SuppressWarnings({ "ConstantConditions" })
    @Test(expected = NullPointerException.class)
    public void aNullPointerIsThrownWhenANullIssuingPlayerIdIsSentToTheService() {
        underTest.invitationSent(null, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);
    }

    @SuppressWarnings({ "ConstantConditions" })
    @Test(expected = NullPointerException.class)
    public void aNullPointerIsThrownWhenANullRecipientIdentifierIsSentToTheService() {
        underTest.invitationSent(ISSUING_PLAYER_ID, null, RECIPIENT_SOURCE, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);
    }

    @SuppressWarnings({ "ConstantConditions" })
    @Test(expected = NullPointerException.class)
    public void aNullPointerExceptionsIsThrownWhenANullRecipientSourceIsSentToTheService() {
        underTest.invitationSent(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, null, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);
    }

    @SuppressWarnings({ "ConstantConditions" })
    @Test(expected = NullPointerException.class)
    public void aIllegalArgumentExceptionsIsThrownWhenANullCreatedTimeIsSentToTheService() {
        underTest.invitationSent(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, null, GAME_TYPE,
                SCREEN_SOURCE);
    }

    @Test
    public void shouldPublishInvitationAccepted() {
        underTest.invitationAccepted(RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, REGISTRATION_TIME, RECIPIENT_PLAYER_ID);

        verify(queuePublishingService).send(
                new InvitationAcceptedMessage(RECIPIENT_IDENTIFIER, RECIPIENT_SOURCE, REGISTRATION_TIME, RECIPIENT_PLAYER_ID));
    }

}
