package com.yazino.game.api;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExecutionContextTest {
    private GameInformation gameInfo;
    private GamePlayerWalletFactory gamePlayerWalletFactory;
    private String auditLabel;
    private ExternalGameService externalGameService;

    @Before
    public void setUp() {
        gameInfo = mock(GameInformation.class);
        externalGameService = mock(ExternalGameService.class);
        gamePlayerWalletFactory = mock(GamePlayerWalletFactory.class);
        auditLabel = "auditLabel";
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotCreateWithoutGameInformation() {
        new ExecutionContext(null, gamePlayerWalletFactory, externalGameService, auditLabel);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotCreateWithoutGameWallet() {
        new ExecutionContext(gameInfo, null, externalGameService, auditLabel);
    }

    @Test(expected = NullPointerException.class)
    public void shouldNotCreateWithoutAuditLabel() {
        new ExecutionContext(gameInfo, gamePlayerWalletFactory, externalGameService, null);
    }

    @Test
    public void shouldGetGameInformation() {
        final GameStatus status = mock(GameStatus.class);
        final Long gameId = 99L;
        when(gameInfo.getCurrentGame()).thenReturn(status);
        when(gameInfo.getGameId()).thenReturn(gameId);
        when(gameInfo.isAddingPlayersPossible()).thenReturn(true);

        final ExecutionContext unit = new ExecutionContext(gameInfo, gamePlayerWalletFactory, externalGameService, auditLabel);

        assertEquals(status, unit.getGameStatus());
        assertEquals(gameId, unit.getGameId());
        assertEquals(true, unit.isAddingPlayersPossible());
    }

}
