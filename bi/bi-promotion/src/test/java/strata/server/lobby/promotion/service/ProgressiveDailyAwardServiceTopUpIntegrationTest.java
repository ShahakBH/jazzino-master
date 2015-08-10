package strata.server.lobby.promotion.service;

import com.yazino.platform.Platform;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerService;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import strata.server.lobby.api.promotion.DailyAwardPromotionService;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;
import strata.server.lobby.api.promotion.domain.builder.PlayerPromotionStatusBuilder;
import strata.server.lobby.api.promotion.message.TopUpRequest;
import strata.server.lobby.promotion.persistence.PlayerPromotionStatusDao;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "testTxManager")
public class ProgressiveDailyAwardServiceTopUpIntegrationTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(-10);
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(-10);
    @Autowired
    @Qualifier(value = "underTest")
    private DailyAwardPromotionService underTest;

    @Autowired
    private PlayerPromotionStatusDao playerPromotionStatusDao;

    @Autowired
    private PlayerService playerService;

    @Autowired
    @Qualifier("marketingJdbcTemplate")
    private JdbcTemplate marketingJdbcTemplate;

    @Before
    @After
    public void cleanUp() {
        marketingJdbcTemplate.update("DELETE FROM PLAYER_PROMOTION_STATUS WHERE PLAYER_ID = ?", PLAYER_ID);
        marketingJdbcTemplate.update("DELETE FROM PROMOTION_PLAYER_REWARD WHERE PLAYER_ID = ?", PLAYER_ID);
    }

    @Test
    @Transactional
    public void awardDailyTopUpShouldOnlyTopUpOnceWhenManyConcurrentRequests() throws WalletServiceException {
        // ensure last topup date was over 24 hours ago
        final PlayerPromotionStatus playerPromotionStatus = playerPromotionStatusDao.get(PLAYER_ID);
        playerPromotionStatusDao.save(new PlayerPromotionStatusBuilder(playerPromotionStatus)
                .withLastTopupDate(new DateTime().minusDays(2)).build());

        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    underTest.awardDailyTopUp(new TopUpRequest(PLAYER_ID, Platform.WEB, new DateTime(), SESSION_ID));
                }
            };
            executorService.execute(task);
        }
        try {
            executorService.shutdown();
            executorService.awaitTermination(100, TimeUnit.SECONDS);

            Mockito.verify(playerService, times(1)).postTransaction(
                    any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), anyString(), anyString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
