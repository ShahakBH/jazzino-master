package com.yazino.platform.processor.statistic;

import com.google.common.collect.Maps;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.statistic.PlayerGameStatistics;
import com.yazino.platform.model.statistic.PlayerStatisticEvent;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.statistic.PlayerGameStatisticConsumerRepository;
import com.yazino.platform.repository.table.ClientRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.game.api.statistic.GameStatisticConsumer;
import com.yazino.game.api.statistic.GameStatistics;
import com.yazino.game.api.statistic.StatisticEvent;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PlayerGameStatisticsProcessorTest {
    private static final String CLIENT_ID = "aClientId";
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(3434);
    private static final String GAME_TYPE = "aGameType";
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(4242);
    private static final StatisticEvent STATISTIC_EVENT_1 = new StatisticEvent("STATISTIC_EVENT_1");
    private static final StatisticEvent STATISTIC_EVENT_2 = new StatisticEvent("STATISTIC_EVENT_2");
    private static final StatisticEvent STATISTIC_EVENT_3 = new StatisticEvent("STATISTIC_EVENT_3");

    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private GameStatisticConsumer consumer1;
    @Mock
    private OtherGameStatisticConsumer consumer2;
    @Mock
    private PlayerRepository playerRepository;

    private Client client;
    private PlayerGameStatistics playerGameStatistics;

    private GameStatistics statistics;
    private PlayerGameStatisticsProcessor unit;
    private GamePlayer gamePlayer;

    @SuppressWarnings({"unchecked"})
    @Before
    public void setUp() {
        final PlayerGameStatisticConsumerRepository producerRepository = mock(PlayerGameStatisticConsumerRepository.class);
        when(producerRepository.getConsumers()).thenReturn(asList(consumer1, consumer2));

        unit = new PlayerGameStatisticsProcessor(gigaSpace, clientRepository, playerRepository, producerRepository);

        final Map<String, String> clientProperties = new HashMap<String, String>();
        clientProperties.put("property1", "value1");
        clientProperties.put("property2", "value2");
        client = new Client(CLIENT_ID, 5, "test.swf", GAME_TYPE, clientProperties);

        when(clientRepository.findById(CLIENT_ID)).thenReturn(client);
        when(playerRepository.findById(PLAYER_ID)).thenReturn(
                new Player(PLAYER_ID, "aPlayer", BigDecimal.TEN, "aPictureUrl", null, null, null));
        gamePlayer = new GamePlayer(PLAYER_ID, null, "aPlayer");

        final List<GameStatistic> stats = Arrays.asList(
                new GameStatistic(PLAYER_ID, "stat1"), new GameStatistic(PLAYER_ID, "stat2"));
        statistics = new GameStatistics(stats);
        playerGameStatistics = new PlayerGameStatistics(PLAYER_ID, TABLE_ID, GAME_TYPE, CLIENT_ID, stats);

        when(consumer1.consume(any(GamePlayer.class), any(BigDecimal.class), eq(GAME_TYPE), any(Map.class), any(GameStatistics.class)))
                .thenReturn(newHashSet(STATISTIC_EVENT_1, STATISTIC_EVENT_2));
        when(consumer2.consume(any(GamePlayer.class), any(BigDecimal.class), eq(GAME_TYPE), any(Map.class), any(GameStatistics.class)))
                .thenReturn(newHashSet(STATISTIC_EVENT_3));
    }

    @Test
    public void processorShouldCallAllInterestedConsumers() {
        when(consumer1.acceptsGameType(GAME_TYPE)).thenReturn(true);
        when(consumer2.acceptsGameType(GAME_TYPE)).thenReturn(false);

        unit.processRequest(playerGameStatistics);

        verify(consumer1).consume(gamePlayer, TABLE_ID, GAME_TYPE, client.getClientProperties(), statistics);
        verify(consumer2).acceptsGameType(GAME_TYPE);
        verifyNoMoreInteractions(consumer2);
    }

    @Test
    public void processorShouldCallAllInterestedConsumersForRequestsWithNoClient() {
        when(consumer1.acceptsGameType(GAME_TYPE)).thenReturn(true);
        when(consumer2.acceptsGameType(GAME_TYPE)).thenReturn(false);
        playerGameStatistics.setClientId(null);

        unit.processRequest(playerGameStatistics);

        verify(consumer1).consume(gamePlayer, TABLE_ID, GAME_TYPE, Maps.<String, String>newHashMap(), statistics);
        verify(consumer2).acceptsGameType(GAME_TYPE);
        verifyNoMoreInteractions(consumer2);
    }

    @Test
    public void playerAchievementEventsAreWrittenToTheSpaceOnCompletion() {
        when(consumer1.acceptsGameType(GAME_TYPE)).thenReturn(true);
        when(consumer2.acceptsGameType(GAME_TYPE)).thenReturn(true);

        unit.processRequest(playerGameStatistics);

        verify(gigaSpace).write(new PlayerStatisticEvent(PLAYER_ID, GAME_TYPE,
                newHashSet(STATISTIC_EVENT_1, STATISTIC_EVENT_2, STATISTIC_EVENT_3)));
    }

    @Test
    public void eventsAreStillWrittenToTheSpaceWhenTheConsumersGenerateNone() {
        unit.processRequest(playerGameStatistics);

        verify(gigaSpace).write(new PlayerStatisticEvent(PLAYER_ID, GAME_TYPE, new HashSet<StatisticEvent>()));
    }

    @Test
    public void processorShouldRetrieveClientFromRepository() {
        unit.processRequest(playerGameStatistics);

        verify(clientRepository).findById(CLIENT_ID);
    }

    @Test
    public void processorShouldNotCallConsumersIfClientIsInvalid() {
        reset(clientRepository);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(null);

        unit.processRequest(playerGameStatistics);

        verify(consumer1, times(0)).consume(any(GamePlayer.class), any(BigDecimal.class), anyString(), anyMap(), any(GameStatistics.class));
        verify(consumer2, times(0)).consume(any(GamePlayer.class), any(BigDecimal.class), anyString(), anyMap(), any(GameStatistics.class));
    }

    private interface OtherGameStatisticConsumer extends GameStatisticConsumer {
    }
}
