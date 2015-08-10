package com.yazino.platform.invitation.consumer;

import com.yazino.platform.invitation.InvitationSource;
import com.yazino.platform.invitation.emailService.EmailInvitationService;
import com.yazino.platform.invitation.message.InvitationAcceptedMessage;
import com.yazino.platform.invitation.message.InvitationMessage;
import com.yazino.platform.invitation.message.InvitationMessageType;
import com.yazino.platform.invitation.message.InvitationSentMessage;
import com.yazino.platform.invitation.service.InvitationTrackingService;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class InvitationMessageListenerTest {

    private static final BigDecimal ISSUING_PLAYER_ID = BigDecimal.valueOf(100L);
    private static final BigDecimal RECIPIENT_PLAYER_ID = BigDecimal.valueOf(400L);
    private static final String RECIPIENT_IDENTIFIER = "auser@example.com";
    private static final InvitationSource RECIPIENT_SOURCE = InvitationSource.EMAIL;
    private static final String GAME_TYPE = "GAME_TYPE";
    private static final String SCREEN_SOURCE = "FB_SCREEN";
    private static final DateTime CREATED_TIME = new DateTime(2011, 4, 5, 12, 12, 12, 120);
    private static final DateTime REGISTERED_TIME = new DateTime(2011, 6, 7, 8, 9, 10, 110);

    private InvitationTrackingService invitationTrackingService = mock(InvitationTrackingService.class);

    private InvitationMessageListener underTest;

    private EmailInvitationService emailInvitationService;

    @Before
    public void setUp() throws Exception {
        emailInvitationService = mock(EmailInvitationService.class);
        underTest = new InvitationMessageListener(invitationTrackingService, emailInvitationService);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(1);
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void serviceCannotBeCreatedWithANullInvitationService() {
        new InvitationMessageListener(null, emailInvitationService);
    }

    @Test
    public void aNullDeliveryIsIgnored() {
        underTest.handleMessage((InvitationMessage) null);

        verifyZeroInteractions(invitationTrackingService);
    }

    @Test
    public void ifNotConvertedThenTheMessageIsIgnored() {
        underTest.handleMessage(new byte[10]);

        verifyZeroInteractions(invitationTrackingService);
    }

    @Test
    public void ifNoMessageTypeIsPresentThenTheMessageIsIgnored() {
        underTest.handleMessage(new UntypedMessage());

        verifyZeroInteractions(invitationTrackingService);
    }

    @Test
    public void invitationSent_ifInvalidMessageThenTheMessageIsIgnored() {
        underTest.handleMessage(new InvitationSentMessage());

        verifyZeroInteractions(invitationTrackingService);
    }

    @Test
    public void invitationAccepted_ifInvalidMessageThenTheMessageIsIgnored() {
        underTest.handleMessage(new InvitationAcceptedMessage());

        verifyZeroInteractions(invitationTrackingService);
    }

    @Test(expected = RuntimeException.class)
    public void invitationSent_anExceptionDuringMessageProcessingIsPropagated() {
        doThrow(new RuntimeException("aTestException")).when(invitationTrackingService)
                .invitationSent(any(BigDecimal.class), anyString(), any(InvitationSource.class), any(DateTime.class),
                        anyString(), anyString());

        underTest.handleMessage(anInvitationSentMessage());
    }

    @Test(expected = RuntimeException.class)
    public void invitationAccepted_anExceptionDuringMessageProcessingIsPropagated() {
        doThrow(new RuntimeException("aTestException")).when(invitationTrackingService)
                .invitationAccepted(anyString(), any(InvitationSource.class), any(DateTime.class),
                        any(BigDecimal.class));

        underTest.handleMessage(anInvitationAcceptedMessage());
    }

    private InvitationMessage anInvitationSentMessage() {
        return new InvitationSentMessage(ISSUING_PLAYER_ID, RECIPIENT_IDENTIFIER,
                RECIPIENT_SOURCE, CREATED_TIME, GAME_TYPE, SCREEN_SOURCE);
    }

    private InvitationMessage anInvitationAcceptedMessage() {
        return new InvitationAcceptedMessage(
                RECIPIENT_IDENTIFIER,
                RECIPIENT_SOURCE, REGISTERED_TIME, RECIPIENT_PLAYER_ID);
    }

    private static class UntypedMessage implements InvitationMessage {
        private static final long serialVersionUID = -8251707238255676839L;

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public InvitationMessageType getMessageType() {
            return null;
        }
    }
}
