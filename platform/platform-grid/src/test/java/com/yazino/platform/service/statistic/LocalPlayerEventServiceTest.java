package com.yazino.platform.service.statistic;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.event.message.PlayerLevelEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalPlayerEventServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(10);
    private static final int LEVEL = 10;
    private static final BigDecimal BONUS_AMOUNT = BigDecimal.valueOf(100);

    @Mock
    private PlayerService playerService;
    @Mock
    private QueuePublishingService<PlayerLevelEvent> playerLevelEventService;

    private LocalPlayerEventService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new LocalPlayerEventService(playerService, playerLevelEventService);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullPlayerService() {
        new LocalPlayerEventService(null, playerLevelEventService);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullPlayerLevelEventService() {
        new LocalPlayerEventService(playerService, null);
    }

    @Test
    public void publishingANewLevelAwardsChipsToTheSpecifiedPlayer() throws WalletServiceException {
        underTest.publishNewLevel(PLAYER_ID, "aGameType", LEVEL, BONUS_AMOUNT);

        verify(playerService).postTransaction(PLAYER_ID, null, BONUS_AMOUNT, "Level Bonus", "aGameType level 10");
    }

    @Test
    public void publishingANewLevelDoesNotPropagateExceptionsFromTheWalletService() throws WalletServiceException {
        when(playerService.postTransaction(PLAYER_ID, null, BONUS_AMOUNT, "Level Bonus", "aGameType level 10"))
                .thenThrow(new WalletServiceException("aTestException"));

        underTest.publishNewLevel(PLAYER_ID, "aGameType", LEVEL, BONUS_AMOUNT);
    }

    @Test
    public void publishingANewLevelSendsAPlayerLevelEvent() throws WalletServiceException {
        underTest.publishNewLevel(PLAYER_ID, "aGameType", LEVEL, BONUS_AMOUNT);

        verify(playerLevelEventService).send(new PlayerLevelEvent(PLAYER_ID.toPlainString(), "aGameType", "10"));
    }

}
