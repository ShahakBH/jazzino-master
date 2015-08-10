package com.yazino.engagement.facebook;

import com.yazino.engagement.campaign.AppRequestExternalReference;
import com.yazino.engagement.FacebookMessageType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FacebookDeleteRequestConsumerTest {
    private FacebookDeleteRequestConsumer underTest;

    @Mock
    private FacebookRequestSender facebookRequestSender;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(facebookRequestSender.sendRequest(any(FacebookMessageType.class), any(FacebookAppRequestEnvelope.class))).thenReturn(new FacebookResponse(FacebookAppToUserRequestStatus.SENT, "123"));
        underTest = new FacebookDeleteRequestConsumer(facebookRequestSender);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfSenderIsNull() {
        new FacebookDeleteRequestConsumer(null);
    }

    @Test
    public void handleShouldSendFacebookDeleteRequest() {
        FacebookDeleteAppRequestMessage message = new FacebookDeleteAppRequestMessage(
                new AppRequestExternalReference("16256125", "SLOTS", "3087768"));

        underTest.handle(message);

        verify(facebookRequestSender).deleteRequest(message.getAppRequestExternalReference());
    }
}
