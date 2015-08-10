package strata.server.lobby.promotion.service;


import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.promotion.DailyAwardPromotionService;
import strata.server.lobby.api.promotion.message.TopUpAcknowledgeRequest;
import strata.server.lobby.api.promotion.message.TopUpRequest;
import strata.server.lobby.promotion.persistence.PlayerPromotionStatusDao;

import java.math.BigDecimal;

import static com.yazino.platform.Platform.IOS;
import static org.mockito.Mockito.verify;

public class TopUpServiceTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final BigDecimal SESSION_ID = BigDecimal.TEN;
    @Mock
    DailyAwardPromotionService dailyAwardPromotionService;

    @Mock
    PlayerPromotionStatusDao playerPromotionStatusDao;

    private TopUpService underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        underTest = new TopUpService(dailyAwardPromotionService, playerPromotionStatusDao);
    }

    @Test(expected = NullPointerException.class)
    public void aTopUpServiceCannotBeCreatedWithANullDailyAwardPromotionService() {
        new TopUpService(null, playerPromotionStatusDao);
    }

    @Test
    public void topUpServiceShouldDelegateToTheDailyAwardPromotionService(){
        final DateTime requestDate = new DateTime();
        underTest.topUpPlayer(new TopUpRequest(BigDecimal.TEN, IOS, requestDate, SESSION_ID));
        verify(dailyAwardPromotionService).awardDailyTopUp(new TopUpRequest(PLAYER_ID, IOS, requestDate, SESSION_ID));
    }

    @Test
    public void verifyTopUpPlayerHandlesExceptionsSoThatMessagesAreNotPutBackOntoTheQueue() {
        Mockito.doThrow(new RuntimeException("Random Exception")).when(dailyAwardPromotionService).awardDailyTopUp(
                Mockito.any(TopUpRequest.class));
        underTest.topUpPlayer(new TopUpRequest());
    }

    @Test
    public void acknowledgeTopUpForPlayerShouldCallAcknowledgeTopUp() {
        final DateTime acknowledgeDate = new DateTime();
        underTest.acknowledgeTopUpForPlayer(new TopUpAcknowledgeRequest(PLAYER_ID, acknowledgeDate));

        verify(playerPromotionStatusDao).saveAcknowledgeTopUpForPlayer(PLAYER_ID, acknowledgeDate);
    }

    @Test
    public void verifyAcknowledgeTopUpForPLayerHandlesExceptionsSoThatMessagesAreNotPutBackOntoTheQueue()   {
        final DateTime acknowledgeDate = new DateTime();
        Mockito.doThrow(new RuntimeException("Random Exception")).when(playerPromotionStatusDao).saveAcknowledgeTopUpForPlayer(PLAYER_ID, acknowledgeDate);

        underTest.acknowledgeTopUpForPlayer(new TopUpAcknowledgeRequest(PLAYER_ID, acknowledgeDate));

    }
}
