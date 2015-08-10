package com.yazino.test.statistic.leveling;

import com.yazino.platform.model.statistic.PlayerStatistics;
import com.yazino.platform.playerstatistic.StatisticEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.game.api.statistic.GameStatisticConsumer;
import com.yazino.game.api.statistic.GameStatisticProducer;
import com.yazino.game.api.statistic.GameStatistics;
import com.yazino.platform.processor.statistic.PlayerStatisticEventProcessor;
import com.yazino.platform.model.statistic.PlayerLevel;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.repository.statistic.PlayerLevelsRepository;

import java.math.BigDecimal;
import java.util.*;

public class LevelingIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(LevelingIntegration.class);
    private static final BigDecimal TABLE_ID = BigDecimal.ONE;
    private static final Map<String, String> CLIENT_PROPERTIES = new HashMap<>();

    private final PlayerStatisticEventProcessor statisticEventProcessor;
    private final TestLevelingSystemRepository levelingSystemRepository;
    private final PlayerLevelsRepository playerRepository;

    private String gameType;
    private GameStatisticConsumer gameStatisticConsumer;
    private GameStatisticProducer gameStatisticProducer;

    public LevelingIntegration(final PlayerStatisticEventProcessor statisticEventProcessor,
                               final PlayerLevelsRepository playerRepository,
                               final TestLevelingSystemRepository levelingSystemRepository) {
        this.statisticEventProcessor = statisticEventProcessor;
        this.playerRepository = playerRepository;
        this.levelingSystemRepository = levelingSystemRepository;
    }


    public void setGameStatisticConsumer(final GameStatisticConsumer gameStatisticConsumer) {
        this.gameStatisticConsumer = gameStatisticConsumer;
    }

    public void setGameStatisticProducer(final GameStatisticProducer gameStatisticProducer) {
        this.gameStatisticProducer = gameStatisticProducer;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public void processCompletedGame(final GameStatus status) {
        final Collection<GameStatistic> stats = gameStatisticProducer.processCompletedGame(status);
        final Map<BigDecimal, Collection<GameStatistic>> playerStats = new HashMap<>();
        for (GameStatistic statistic : stats) {
            final BigDecimal playerId = statistic.getPlayerId();
            Collection<GameStatistic> statisticsForPlayer = playerStats.get(playerId);
            if (statisticsForPlayer == null) {
                statisticsForPlayer = new HashSet<>();
                playerStats.put(playerId, statisticsForPlayer);
            }
            statisticsForPlayer.add(statistic);
        }

        for (BigDecimal playerId : playerStats.keySet()) {
            final GameStatistics gameStatistics = new GameStatistics(playerStats.get(playerId));
            final GamePlayer player = new GamePlayer(playerId, null, "playa_" + playerId);
            LOG.debug(String.format("Processing statistics for %s: %s ", player.getName(), playerStats.get(playerId)));
            final Set<com.yazino.game.api.statistic.StatisticEvent> events = gameStatisticConsumer.consume(
                    player, TABLE_ID, gameType, CLIENT_PROPERTIES, gameStatistics);
            final Set<StatisticEvent> finalEvents = new HashSet<>();
            for (com.yazino.game.api.statistic.StatisticEvent event : events) {
                finalEvents.add(new StatisticEvent(event.getEvent(),
                        event.getDelay(),
                        event.getMultiplier(),
                        event.getParameters()));
            }

            final PlayerStatistics message = new PlayerStatistics(playerId, gameType, finalEvents);
            statisticEventProcessor.process(message);
        }
    }

    public String getExperience(final BigDecimal playerId) {
        final PlayerLevel playerLevel = playerRepository.forPlayer(playerId).retrievePlayerLevel(getGameType());
        return String.valueOf(playerLevel.getExperience().intValue());
    }

    public void withPlayers(final GamePlayer... players) {
        for (GamePlayer player : players) {
            final PlayerLevels playerLevels = new PlayerLevels(player.getId(),
                    new HashMap<String, PlayerLevel>());
            playerRepository.save(playerLevels);
        }
    }

    public TestLevelingSystemRepository getLevelingSystemRepository() {
        return levelingSystemRepository;
    }

    public String getGameType() {
        return gameType;
    }
}
