package com.yazino.platform.processor.tournament;

import com.yazino.platform.repository.table.GameTypeRepository;
import com.yazino.platform.service.statistic.NewsEventPublisher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.GameFeature;
import com.yazino.game.api.GameMetaDataBuilder;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.GameType;
import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.game.api.statistic.GameStatistics;
import com.yazino.game.api.statistic.StatisticEvent;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static com.yazino.game.api.GameMetaDataKey.TOURNAMENT_RANKING_MESSAGE;
import static com.yazino.game.api.GameMetaDataKey.TOURNAMENT_SUMMARY_MESSAGE;

public class GameTournamentStatisticConsumerTest {
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(4353);

    @Mock
    private NewsEventPublisher newsEventPublisher;
    @Mock
    private GameTypeRepository gameTypeRepository;

    private final Map<String, TournamentFinalPositionStrategy> strategiesForGameTypes = new HashMap<>();

    private GameTournamentStatisticConsumer underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(gameTypeRepository.getGameType("GAME1")).thenReturn(new GameType("GAME1", "Game 1", newHashSet("game1"), newHashSet(GameFeature.TOURNAMENT)));
        when(gameTypeRepository.getGameType("GAME2")).thenReturn(new GameType("GAME2", "Game 2", newHashSet("game2")));
        when(gameTypeRepository.getGameType("GAME3")).thenReturn(new GameType("GAME3", "Game 3", newHashSet("game2"), newHashSet(GameFeature.TOURNAMENT)));

        when(gameTypeRepository.getMetaDataFor("GAME1")).thenReturn(new GameMetaDataBuilder()
                .with(TOURNAMENT_RANKING_MESSAGE, "G1: %s has ranked %s of %s")
                .with(TOURNAMENT_SUMMARY_MESSAGE, "G1: You've ranked %s").build());
        when(gameTypeRepository.getMetaDataFor("GAME3")).thenReturn(new GameMetaDataBuilder()
                .with(TOURNAMENT_RANKING_MESSAGE, "G3: %s has ranked %s of %s")
                .with(TOURNAMENT_SUMMARY_MESSAGE, "G3: You've ranked %s").build());

        strategiesForGameTypes.put("GAME1", mock(TournamentFinalPositionStrategy.class));
        strategiesForGameTypes.put("GAME3", mock(TournamentFinalPositionStrategy.class));

        underTest = new TestGameTournamentStatisticConsumer(newsEventPublisher, gameTypeRepository);
    }

    @Test
    public void gameTypesThatSupportTournamentsShouldBeAccepted() {
        assertThat(underTest.acceptsGameType("GAME1"), is(true));
        assertThat(underTest.acceptsGameType("GAME3"), is(true));
    }

    @Test
    public void gameTypesThatDoNotSupportTournamentsShouldNotBeAccepted() {
        assertThat(underTest.acceptsGameType("GAME2"), is(false));
    }

    @Test
    public void eventsAreConsumedCorrectlyForGameTypesThatSupportTournamentsAndReturnEvents() {
        when(strategiesForGameTypes.get("GAME1").consume(aGamePlayer(), gameStatistics()))
                .thenReturn(aStatisticEvent());

        final Set<StatisticEvent> resultingEvents = underTest.consume(
                aGamePlayer(), TABLE_ID, "GAME1", clientProperties(), gameStatistics());

        assertThat(resultingEvents.size(), is(equalTo(1)));
        assertThat(resultingEvents, hasItem(aStatisticEvent()));
    }

    @Test
    public void eventsAreConsumedCorrectlyForGameTypesThatSupportTournamentsAndReturnNoEvents() {
        final Set<StatisticEvent> resultingEvents = underTest.consume(
                aGamePlayer(), TABLE_ID, "GAME1", clientProperties(), gameStatistics());

        assertThat(resultingEvents.size(), is(equalTo(0)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void eventsForGameTypesThatDoNotSupportTournamentsCauseAnIllegalArgumentException() {
        underTest.consume(aGamePlayer(), TABLE_ID, "GAME2", clientProperties(), gameStatistics());
    }

    @Test
    public void eventsAreConsumedByTheCorrectStatisticConsumer() {
        when(strategiesForGameTypes.get("GAME3").consume(aGamePlayer(), gameStatistics()))
                .thenReturn(aStatisticEvent());

        underTest.consume(aGamePlayer(), TABLE_ID, "GAME3", clientProperties(), gameStatistics());

        verify(strategiesForGameTypes.get("GAME3")).consume(aGamePlayer(), gameStatistics());
        verifyZeroInteractions(strategiesForGameTypes.get("GAME1"));
    }

    private StatisticEvent aStatisticEvent() {
        return new StatisticEvent("anEvent");
    }

    private GameStatistics gameStatistics() {
        return new GameStatistics(Collections.<GameStatistic>emptySet());
    }

    private Map<String, String> clientProperties() {
        return Collections.emptyMap();
    }

    private GamePlayer aGamePlayer() {
        return new GamePlayer(BigDecimal.valueOf(100), null, "aPlayer");
    }

    private class TestGameTournamentStatisticConsumer extends GameTournamentStatisticConsumer {
        public TestGameTournamentStatisticConsumer(@Qualifier("newsEventPublisher") final NewsEventPublisher newsEventPublisher,
                                                   final GameTypeRepository gameTypeRepository) {
            super(newsEventPublisher, gameTypeRepository);
        }

        @Override
        TournamentFinalPositionStrategy initialiseStrategy(final String gameTypeId,
                                                           final String tournamentRankingMessage,
                                                           final String tournamentRankingShortMessage) {
            return strategiesForGameTypes.get(gameTypeId);
        }
    }

}
