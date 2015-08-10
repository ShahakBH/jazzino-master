package strata.server.lobby.promotion.service;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;
import strata.server.lobby.api.promotion.domain.builder.PlayerPromotionStatusBuilder;
import strata.server.lobby.promotion.persistence.PlayerPromotionStatusDao;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProgressiveDailyAwardTestingServiceTest {
    public static final BigDecimal PLAYER_ID = new BigDecimal(-12934);
    ProgressiveDailyAwardTestingService underTest;

    @Mock
    PlayerPromotionStatusDao playerPromotionStatusDao;

    @Before
    public void setUp() throws Exception {
        underTest = new ProgressiveDailyAwardTestingService(playerPromotionStatusDao);

    }

    @Test
    public void testGetPlayerPromotionStatus() throws Exception {

        when(playerPromotionStatusDao.get(PLAYER_ID)).thenReturn(new PlayerPromotionStatusBuilder().build());

        PlayerPromotionStatus expectedPlayerPromotionStatus = new PlayerPromotionStatusBuilder().build();
        PlayerPromotionStatus actualPlayerPromotionStatus = underTest.getPlayerPromotionStatus(PLAYER_ID);

        assertEquals(expectedPlayerPromotionStatus, actualPlayerPromotionStatus);
    }

    @Test
    public void testSetDailyAwardUpdatesPlayerPromotionStatus() throws Exception {
        PlayerPromotionStatus playerPromotionStatus = new PlayerPromotionStatusBuilder()
                                                        .withLastPlayed(new DateTime())
                                                        .withLastTopupDate(new DateTime().minusDays(1))
                                                        .withConsecutiveDaysPlayed(2)
                                                        .build();

        underTest.setDailyAwardStatus(playerPromotionStatus);
        verify(playerPromotionStatusDao).save(playerPromotionStatus);

    }
}
