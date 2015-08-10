package com.yazino.platform.processor.table;

import com.yazino.platform.model.table.GameCompleted;
import com.yazino.platform.repository.statistic.PlayerGameStatisticProducerRepository;
import com.yazino.platform.service.statistic.GameStatisticsPublisher;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.game.api.statistic.GameStatisticProducer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
public class CompletedGameProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(CompletedGameProcessor.class);

    private final GameStatisticsPublisher statisticsPublisher;
    private final PlayerGameStatisticProducerRepository playerGameStatisticProducerRepository;

    // CGLib construc
    protected CompletedGameProcessor() {
        this.statisticsPublisher = null;
        this.playerGameStatisticProducerRepository = null;
    }

    @Autowired
    public CompletedGameProcessor(final GameStatisticsPublisher statisticsPublisher,
                                  final PlayerGameStatisticProducerRepository playerGameStatisticProducerRepository) {
        notNull(statisticsPublisher, "gameStatisticsPublisher is null");
        notNull(playerGameStatisticProducerRepository, "playerGameStatisticProducerRepository is null");

        this.statisticsPublisher = statisticsPublisher;
        this.playerGameStatisticProducerRepository = playerGameStatisticProducerRepository;
    }

    @EventTemplate
    public GameCompleted receivedTemplate() {
        return new GameCompleted();
    }

    @SpaceDataEvent
    public void process(final GameCompleted completedGame) {
        notNull(statisticsPublisher, "Invalid constructor used, statisticsStrategies required");

        LOG.debug("processing {}", completedGame);

        final String gameType = completedGame.getGameType();
        final String clientId = completedGame.getClientId();
        final BigDecimal tableId = completedGame.getTableId();

        final Collection<GameStatistic> statisticCollection = new ArrayList<GameStatistic>();
        for (GameStatisticProducer statisticsStrategy : playerGameStatisticProducerRepository.getProducers()) {
            if (statisticsStrategy.getGameType().equalsIgnoreCase(gameType)) {
                LOG.debug("Executing strategy {}", statisticsStrategy);

                final Collection<GameStatistic> statistics = statisticsStrategy.processCompletedGame(
                        completedGame.getGameStatus());
                LOG.debug("Strategy {} generated statistics {}", statisticsStrategy, statistics);
                statisticCollection.addAll(statistics);
            }
        }

        LOG.debug("publishing {}", statisticCollection);
        statisticsPublisher.publish(tableId, gameType, clientId, statisticCollection);
    }
}
