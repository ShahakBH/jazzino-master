package com.yazino.test;


import com.yazino.test.game.GameHostFactory;
import com.yazino.test.game.GameMessageDispatcher;
import com.yazino.test.game.InMemoryWalletPlayerService;
import com.yazino.test.game.TestRandomizer;
import com.yazino.test.statistic.BlackholeNotificationService;
import com.yazino.test.statistic.InMemoryGameStatisticsPublisher;
import com.yazino.test.statistic.TestAchievementRepository;
import com.yazino.test.statistic.achievement.AchievementIntegration;
import com.yazino.test.statistic.achievement.TestPlayerAchievementsRepository;
import com.yazino.test.statistic.leveling.*;
import com.yazino.test.wallet.IntegrationWalletService;
import com.yazino.game.api.GameRules;
import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.test.InMemoryPlayerRepository;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.processor.statistic.PlayerStatisticEventConsumer;
import com.yazino.platform.processor.statistic.PlayerStatisticEventProcessor;
import com.yazino.platform.processor.statistic.level.ExperienceStatisticEventsConsumer;
import senet.server.host.local.InMemoryNovomaticGameRequestService;

import java.util.ArrayList;
import java.util.Collection;

public class YazinoIntegrationFactory {

    private final InMemoryGameStatisticsPublisher statisticsPublisher = new InMemoryGameStatisticsPublisher();

    private final GameHostFactory gameHostFactory = new GameHostFactory();

    public GameIntegration forGame(final GameRules gameRules) {
        return forGame(gameRules, new SettableTimeSource(), new TestRandomizer());
    }

    public GameIntegration forGame(final GameRules gameRules,
                                   final SettableTimeSource timeSource,
                                   final TestRandomizer randomizer) {
        final IntegrationWalletService walletService = new IntegrationWalletService(true);
        return forGame(gameRules, walletService, timeSource, randomizer);
    }

    public GameIntegration forGame(final GameRules gameRules,
                                   final IntegrationWalletService walletService) {
        return forGame(gameRules, walletService, new SettableTimeSource(), new TestRandomizer());
    }

    public GameIntegration forGame(final GameRules gameRules,
                                   final IntegrationWalletService walletService,
                                   final SettableTimeSource timeSource,
                                   final TestRandomizer randomizer) {
        final InMemoryPlayerRepository playerRepository = new InMemoryPlayerRepository();
        final InMemoryWalletPlayerService playerService = new InMemoryWalletPlayerService(walletService);
        final InMemoryNovomaticGameRequestService externalGameRequestService = new InMemoryNovomaticGameRequestService();
        final GameHost gameHost = gameHostFactory.create(
                playerRepository,
                timeSource,
                walletService,
                gameRules,
                statisticsPublisher,
                externalGameRequestService);
        return new GameIntegration(playerService,
                walletService,
                externalGameRequestService,
                gameHost,
                new GameMessageDispatcher(),
                playerRepository,
                timeSource,
                statisticsPublisher,
                randomizer,
                gameRules);
    }

    public AchievementIntegration forAchievements() {
        final TestAchievementRepository achievementRepository = new TestAchievementRepository();
        final TestPlayerAchievementsRepository playerAchievementsRepository = new TestPlayerAchievementsRepository();
        final Collection<PlayerStatisticEventConsumer> consumers =
                new ArrayList<PlayerStatisticEventConsumer>();
        final Collection<com.yazino.platform.processor.statistic.achievement.Accumulator> accumulators =
                new ArrayList<com.yazino.platform.processor.statistic.achievement.Accumulator>();
        final com.yazino.platform.processor.statistic.achievement.AchievementManager achievementManager =
                new com.yazino.platform.processor.statistic.achievement.PublishingAchievementManager(new BlackholeNotificationService());
        accumulators.add(new com.yazino.platform.processor.statistic.achievement.SingleEventAccumulator(achievementManager));
        accumulators.add(new com.yazino.platform.processor.statistic.achievement.ThresholdEventAccumulator(achievementManager));
        accumulators.add(new com.yazino.platform.processor.statistic.achievement.ResettingThresholdEventAccumulator(achievementManager));
        final PlayerStatisticEventConsumer achievementStatisticConsumer = new com.yazino.platform.processor.statistic.achievement.AchievementStatisticEventsConsumer(
                accumulators,
                achievementRepository,
                playerAchievementsRepository
        );
        consumers.add(achievementStatisticConsumer);
        final PlayerStatisticEventProcessor processor = new PlayerStatisticEventProcessor(consumers);
        return new AchievementIntegration(processor,
                playerAchievementsRepository,
                achievementRepository,
                statisticsPublisher);
    }

    public LevelingIntegration forLeveling() {
        final TestPlayerLevelsRepository playerRepository = new TestPlayerLevelsRepository();
        final TestLevelingSystemRepository levelingSystemRepository = new TestLevelingSystemRepository();
        final PlayerStatisticEventConsumer levelingStatisticConsumer = new ExperienceStatisticEventsConsumer(
                levelingSystemRepository,
                playerRepository,
                new TestPlayerXPPublisher(),
                new TestPlayerNewLevelPublisher()
        );
        final Collection<PlayerStatisticEventConsumer> consumers = new ArrayList<PlayerStatisticEventConsumer>();
        consumers.add(levelingStatisticConsumer);
        final PlayerStatisticEventProcessor processor = new PlayerStatisticEventProcessor(consumers);
        return new LevelingIntegration(processor,
                playerRepository, levelingSystemRepository);
    }

}
