package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TournamentStatisticProperty;
import com.yazino.platform.model.tournament.TournamentStatisticType;
import com.yazino.platform.repository.table.GameTypeRepository;
import com.yazino.platform.service.statistic.NewsEventPublisher;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.game.api.statistic.GameStatistics;
import com.yazino.game.api.statistic.StatisticEvent;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TournamentFinalPositionStrategyTest {

    private static final String TEST_MESSAGE = "%s finished %s in a competition with %s players";
    private static final String TEST_DESC = "You ranked %s";

    private static final String GAME_TYPE = "GAME17";

    private NewsEventPublisher newsEventPublisher;
    private GamePlayer player = new GamePlayer(BigDecimal.ONE, null, "aPlayer");

    private TournamentFinalPositionStrategy underTest;

    private Map<String, String> properties;

    @Before
    public void setUp() {
        newsEventPublisher = mock(NewsEventPublisher.class);
        final GameTypeRepository gameTypeRepository = mock(GameTypeRepository.class);

        underTest = new TournamentFinalPositionStrategy(newsEventPublisher, gameTypeRepository, GAME_TYPE, TEST_MESSAGE, TEST_DESC);
        properties = new HashMap<String, String>();
    }

    @Test
    public void ifAchievementExistsPublishAchievementEvent() {
        properties.put(TournamentStatisticProperty.POSITION.name(), "1");
        properties.put(TournamentStatisticProperty.NUMBER_OF_PLAYERS.name(), "30");
        final GameStatistics statistics = new GameStatistics(
                Arrays.asList(new GameStatistic(player.getId(), TournamentStatisticType.FINAL_LEADERBOARD_POSITION.name(), properties)));
        assertEquals(new StatisticEvent("GAME17_COMPETITION_1_OUT_OF_20", 0, 1, 29), underTest.consume(player, statistics));
    }

    @Test
    public void ifAchievementDoesntExistsAndPositionBelowMaxPublishSimpleNewsEvent() {
        properties.put(TournamentStatisticProperty.POSITION.name(), "4");
        properties.put(TournamentStatisticProperty.NUMBER_OF_PLAYERS.name(), "30");
        final GameStatistics statistics = new GameStatistics(Arrays.asList(new GameStatistic(player.getId(), TournamentStatisticType.FINAL_LEADERBOARD_POSITION.name(), properties)));
        assertNull(underTest.consume(player, statistics));
        verify(newsEventPublisher).send(new NewsEvent.Builder(player.getId(), new ParameterisedMessage(TEST_MESSAGE, player.getName(), 4, 30))
                .setType(NewsEventType.NEWS)
                .setShortDescription(new ParameterisedMessage(TEST_DESC, 4))
                .setImage("GAME17-tournament-position-4")
                .setDelay(0)
                .build());
    }

    @Test
    public void ifLessThan10PlayersJustMaxPublishSimpleNewsEvent() {
        properties.put(TournamentStatisticProperty.POSITION.name(), "1");
        properties.put(TournamentStatisticProperty.NUMBER_OF_PLAYERS.name(), "8");
        final GameStatistics statistics = new GameStatistics(
                Arrays.asList(new GameStatistic(player.getId(), TournamentStatisticType.FINAL_LEADERBOARD_POSITION.name(), properties)));
        assertNull(underTest.consume(player, statistics));
        verify(newsEventPublisher).send(new NewsEvent.Builder(player.getId(), new ParameterisedMessage(TEST_MESSAGE, player.getName(), 1, 8))
                .setType(NewsEventType.NEWS)
                .setShortDescription(new ParameterisedMessage(TEST_DESC, 1))
                .setImage("GAME17-tournament-position-1")
                .setDelay(0)
                .build());
    }

    @Test
    public void shouldIgnoreIfFinalLeaderboardPositionNotPresent() {
        final GameStatistics statistics = new GameStatistics(new HashSet<GameStatistic>());
        assertNull(underTest.consume(player, statistics));
    }

    @Test
    public void shouldIgnoreIfStatisticDetailsNotPresent() {
        properties.put(TournamentStatisticProperty.POSITION.name(), "1");
        final GameStatistics statistics = new GameStatistics(
                Arrays.asList(new GameStatistic(player.getId(), TournamentStatisticType.FINAL_LEADERBOARD_POSITION.name())));
        assertNull(underTest.consume(player, statistics));
    }
}
