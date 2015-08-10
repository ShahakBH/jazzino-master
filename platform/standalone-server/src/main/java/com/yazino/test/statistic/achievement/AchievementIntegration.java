package com.yazino.test.statistic.achievement;

import com.yazino.platform.model.statistic.PlayerStatistics;
import com.yazino.platform.playerstatistic.StatisticEvent;
import com.yazino.test.statistic.InMemoryGameStatisticsPublisher;
import com.yazino.test.statistic.SimplifiedAchievementDefinition;
import com.yazino.test.statistic.TestAchievementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.GameStatus;
import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.game.api.statistic.GameStatisticConsumer;
import com.yazino.game.api.statistic.GameStatisticProducer;
import com.yazino.game.api.statistic.GameStatistics;
import com.yazino.platform.processor.statistic.PlayerStatisticEventProcessor;
import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.model.statistic.Achievement;

import java.math.BigDecimal;
import java.util.*;


public class AchievementIntegration {

    private static final Logger LOG = LoggerFactory.getLogger(AchievementIntegration.class);
    private static final BigDecimal TABLE_ID = BigDecimal.ONE;
    private static final Map<String, String> CLIENT_PROPERTIES = new HashMap<String, String>();

    private final Map<BigDecimal, GamePlayer> players = new HashMap<BigDecimal, GamePlayer>();
    private final PlayerStatisticEventProcessor statisticEventProcessor;
    private final TestPlayerAchievementsRepository playerAchievementsRepository;
    private GameStatisticProducer currentStrategy;
    private final TestAchievementRepository achievementRepository;
    private final InMemoryGameStatisticsPublisher gameStatisticsPublisher;
    private GameStatisticConsumer currentConsumer;

    private String gameType;

    public AchievementIntegration(final PlayerStatisticEventProcessor statisticEventProcessor,
                                  final TestPlayerAchievementsRepository playerAchievementsRepository,
                                  final TestAchievementRepository achievementRepository,
                                  final InMemoryGameStatisticsPublisher gameStatisticsPublisher) {
        this.statisticEventProcessor = statisticEventProcessor;
        this.playerAchievementsRepository = playerAchievementsRepository;
        this.achievementRepository = achievementRepository;
        this.gameStatisticsPublisher = gameStatisticsPublisher;
    }

    public TestPlayerAchievementsRepository getPlayerAchievementsRepository() {
        return playerAchievementsRepository;
    }

    public Map<BigDecimal, GamePlayer> getPlayers() {
        return players;
    }

    public AchievementIntegration withStrategy(final GameStatisticProducer strategy) {
        this.currentStrategy = strategy;
        gameType = strategy.getGameType();
        return this;
    }


    public AchievementIntegration withConsumer(final GameStatisticConsumer consumer) {
        this.currentConsumer = consumer;
        return this;
    }


    public AchievementIntegration withPlayers(final GamePlayer... playersToAdd) {
        for (GamePlayer gamePlayer : playersToAdd) {
            this.players.put(gamePlayer.getId(), gamePlayer);
            final PlayerAchievements communityPlayer = new PlayerAchievements(gamePlayer.getId(),
                    new HashSet<String>(),
                    new HashMap<String, String>());
            playerAchievementsRepository.save(communityPlayer);
        }
        return this;
    }

    public void addPlayers(final GamePlayer... gamePlayers) {
        withPlayers(gamePlayers);
    }

    public void processCompletedGame(final GameStatus status) {
        final Collection<GameStatistic> statisticCollection = currentStrategy.processCompletedGame(status);
        processStatistics(statisticCollection);

    }

    public Collection<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    public void processStatistics(final Collection<GameStatistic> stats) {
        final Map<BigDecimal, Collection<GameStatistic>> playerStats =
                new HashMap<BigDecimal, Collection<GameStatistic>>();
        for (GameStatistic statistic : stats) {
            final BigDecimal playerId = statistic.getPlayerId();
            Collection<GameStatistic> statisticsForPlayer = playerStats.get(playerId);
            if (statisticsForPlayer == null) {
                statisticsForPlayer = new HashSet<GameStatistic>();
                playerStats.put(playerId, statisticsForPlayer);
            }
            statisticsForPlayer.add(statistic);
        }


        for (BigDecimal playerId : playerStats.keySet()) {
            final GameStatistics gameStatistics = new GameStatistics(playerStats.get(playerId));
            final GamePlayer player = players.get(playerId);
            final Set<com.yazino.game.api.statistic.StatisticEvent> events = currentConsumer.consume(player,
                    TABLE_ID,
                    gameType,
                    CLIENT_PROPERTIES,
                    gameStatistics);
            final Set<StatisticEvent> finalEvents = new HashSet<StatisticEvent>();
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

    public boolean verifyAchievementIsGiven(final GamePlayer player,
                                            final String achievement) {
        return playerAchievementsRepository.forPlayer(player.getId()).getAchievements().contains(achievement);
    }

    public void updateAchievementProgress(final GamePlayer player,
                                          final String achievement,
                                          final String value) {
        final PlayerAchievements communityPlayer = playerAchievementsRepository.forPlayer(player.getId());
        communityPlayer.updateProgressForAchievement(achievement, value);
        playerAchievementsRepository.save(communityPlayer);
    }

    public void awardAchievement(final GamePlayer player,
                                 final String achievement) {
        final PlayerAchievements commPlayer = playerAchievementsRepository.forPlayer(player.getId());
        commPlayer.awardAchievement(achievement);
        playerAchievementsRepository.save(commPlayer);

    }

    public Set<String> getPlayerAchievements(final GamePlayer player) {
        final PlayerAchievements commPlayer = playerAchievementsRepository.forPlayer(player.getId());
        return commPlayer.getAchievements();
    }

    public Map<String, String> getPlayerAchievementProgress(final GamePlayer player) {
        final PlayerAchievements commPlayer = playerAchievementsRepository.forPlayer(player.getId());
        if (commPlayer.getAchievementProgress() != null) {
            return commPlayer.getAchievementProgress();
        }
        return Collections.emptyMap();
    }

    public void clearAchievements() {
        for (BigDecimal playerId : players.keySet()) {
            final PlayerAchievements player = playerAchievementsRepository.forPlayer(playerId);
            if (player != null) {
                final PlayerAchievements newPlayer = new PlayerAchievements(player.getPlayerId(),
                        new HashSet<String>(),
                        new HashMap<String, String>());
                playerAchievementsRepository.save(newPlayer);
            }
        }
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public void withAchievement(final SimplifiedAchievementDefinition definition) {
        achievementRepository.addAchievement(gameType, definition);
    }
}
