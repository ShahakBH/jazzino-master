package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.statistic.PlayerStatisticEvent;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.service.statistic.PlayerStatisticEventsPublisher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.statistic.GameXPEvents;
import com.yazino.game.api.statistic.StatisticEvent;
import com.yazino.game.api.statistic.XPStatisticEventType;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.Mockito.*;

public class GameXPPublishingPostProcessorTest {

    @Mock
    private com.yazino.game.api.GamePlayerWalletFactory gamePlayerWalletFactory;

    private PlayerStatisticEventsPublisher publisher;
    private GameXPPublishingPostProcessor unit;
    private Table table;
    private static final String GAME_TYPE = "getGameType";
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private final List<HostDocument> documentsToSend = new ArrayList<HostDocument>();
    private com.yazino.game.api.GameStatus gameStatus = mock(com.yazino.game.api.GameStatus.class);
    private com.yazino.game.api.GameRules gameRules = mock(com.yazino.game.api.GameRules.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        table = new Table();
        table.setGameTypeId(GAME_TYPE);
        publisher = mock(PlayerStatisticEventsPublisher.class);
        unit = new GameXPPublishingPostProcessor(publisher);
    }

    @Test
    public void shouldPublishEvents() throws com.yazino.game.api.GameException {
        GameXPEvents events = new GameXPEvents();
        events.addEvent(PLAYER_ID, XPStatisticEventType.XP_PLAY);
        com.yazino.game.api.ExecutionResult executionResult = new com.yazino.game.api.ExecutionResult.Builder(gameRules, gameStatus).gameXPEvents(events).build();
        unit.postProcess(executionResult, null, table, null, documentsToSend, null);
        Collection<PlayerStatisticEvent> playerEvents = new HashSet<PlayerStatisticEvent>();
        playerEvents.add(new PlayerStatisticEvent(PLAYER_ID, GAME_TYPE, new HashSet<StatisticEvent>(Arrays.asList(new StatisticEvent(XPStatisticEventType.XP_PLAY.name(), 0, 1)))));
        verify(publisher).publishEvents(playerEvents);
    }

    @Test
    public void ignoreIfNoExecutionResult() throws com.yazino.game.api.GameException {
        unit.postProcess(null, null, table, null, documentsToSend, null);
        verifyNoMoreInteractions(publisher);
    }
}
