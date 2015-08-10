package strata.server.lobby.promotion.consumer;

import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.Platform;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.promotion.message.TopUpAcknowledgeRequest;
import strata.server.lobby.api.promotion.message.TopUpRequest;
import strata.server.lobby.promotion.service.TopUpService;

import java.math.BigDecimal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;


public class PromotionRequestConsumerTest {
    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    public static final BigDecimal SESSION_ID = BigDecimal.TEN;
    @Mock
    TopUpService topUpService;

    private PromotionRequestConsumer underTest;
    private DateTime topUpDate;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        topUpDate = new DateTime();
        underTest = new PromotionRequestConsumer(topUpService);
    }

    @Test(expected = NullPointerException.class)
    public void aPromotionRequestConsumerCannotBeCreatedWithANullDailyAwardService() {
        new PromotionRequestConsumer(null);
    }

    @Test
    public void handleShouldCreditPlayerWithDailyAward() {
        underTest.handle(new TopUpRequest(PLAYER_ID, Platform.IOS, topUpDate, SESSION_ID));

        Mockito.verify(topUpService).topUpPlayer(new TopUpRequest(PLAYER_ID, Platform.IOS, topUpDate, SESSION_ID));
    }

    @Test
    public void handleShouldLogInvalidTopUpRequestAndNotTopUpPlayer() {
        ListAppender listAppender = ListAppender.addTo(PromotionRequestConsumer.class);

        final TopUpRequest invalidRequest = new TopUpRequest(PLAYER_ID, null, null, SESSION_ID);
        underTest.handle(invalidRequest);

        assertTrue(listAppender.getMessages().contains(
                "Could not process invalid top up request [" + invalidRequest + "]"));
        verifyNoMoreInteractions(topUpService);
    }

    @Test
    public void handleShouldSendAcknowledgementsRequestsToDao() {
        final TopUpAcknowledgeRequest topUpAcknowledgeRequest = new TopUpAcknowledgeRequest(PLAYER_ID, topUpDate);
        underTest.handle(topUpAcknowledgeRequest);
        Mockito.verify(topUpService).acknowledgeTopUpForPlayer(topUpAcknowledgeRequest);
    }


}
