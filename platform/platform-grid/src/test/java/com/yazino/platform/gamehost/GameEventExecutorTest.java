package com.yazino.platform.gamehost;

import com.yazino.platform.gamehost.external.NovomaticGameRequestService;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWallet;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWalletFactory;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.service.audit.Auditor;
import com.yazino.platform.service.audit.CommonsLoggingAuditor;
import com.yazino.platform.test.game.status.MockGameStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.GameType;
import com.yazino.game.api.ScheduledEvent;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;

import static com.yazino.platform.test.game.status.MockGameStatus.MockGamePhase;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GameEventExecutorTest {

    @Mock
    private GameRepository gameRepository;
    @Mock
    private BufferedGameHostWalletFactory walletFactory;
    @Mock
    private BufferedGameHostWallet gameHostWallet;
    @Mock
    private GameRules gameRules;
    @Mock
    private NovomaticGameRequestService novomaticRequestService;

    private final Auditor auditor = new CommonsLoggingAuditor();
    private GameEventExecutor underTest;

    @Before
    public void setup() {
        when(walletFactory.create(any(BigDecimal.class))).thenReturn(gameHostWallet);
        when(gameRepository.getGameRules(anyString())).thenReturn(gameRules);

        underTest = new GameEventExecutor(gameRepository, auditor, walletFactory, novomaticRequestService);
    }

    @Test
    public void shouldNotCreateAGameWhenGameInProgress() {
        GameInitialiser gameInitialiser = mock(GameInitialiser.class);
        underTest.setGameInitialiser(gameInitialiser);

        Table table = new Table(new GameType("SLOTS", "Slots", Collections.<String>emptySet()), BigDecimal.ONE, "DefaultSlots", true);
        table.setTableId(BigDecimal.ONE);
        table.setCurrentGame(statusWithPhase(MockGamePhase.Playing));
        table.setGameId(100L);

        underTest.execute(table, new ScheduledEvent(0, table.getGameId(), "", "", Collections.<String, String>emptyMap(), true));
        verify(gameInitialiser).runPreProcessors(eq(gameRules), any(GameInitialiser.GameInitialisationContext.class));
        verifyNoMoreInteractions(gameInitialiser);
    }

    private static GameStatus statusWithPhase(MockGamePhase phase) {
        return new GameStatus(MockGameStatus.emptyStatus(new HashMap<String, String>()).withPhase(phase));
    }
}
