package com.yazino.platform.repository.statistic;

import com.yazino.platform.plugin.statistic.GameStatisticPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.yazino.game.api.statistic.GameStatisticConsumer;
import com.yazino.game.api.statistic.GameStatisticProducer;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class DefaultStatisticRepository implements GameStatisticPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultStatisticRepository.class);

    private final PlayerGameStatisticProducerRepository playerGameStatisticProducerRepository;
    private final PlayerGameStatisticConsumerRepository playerGameStatisticConsumerRepository;

    @Autowired
    public DefaultStatisticRepository(final PlayerGameStatisticProducerRepository playerGameStatisticProducerRepository,
                                      final PlayerGameStatisticConsumerRepository playerGameStatisticConsumerRepository)
            throws Exception {
        notNull(playerGameStatisticProducerRepository, "playerGameStatisticProducerRepository cannot be null");
        notNull(playerGameStatisticConsumerRepository, "playerGameStatisticConsumerRepository cannot be null");

        this.playerGameStatisticProducerRepository = playerGameStatisticProducerRepository;
        this.playerGameStatisticConsumerRepository = playerGameStatisticConsumerRepository;
    }

    @Override
    public void addGameStatisticsProducer(final GameStatisticProducer gameStatisticProducer) {
        LOG.debug("updating game statistics producer for game {}", gameStatisticProducer.getGameType());

        playerGameStatisticProducerRepository.addProducer(gameStatisticProducer);
    }

    @Override
    public void removeGameStatisticsProducer(final GameStatisticProducer gameStatisticProducer) {
        LOG.debug("removing game statistics producer for game {}", gameStatisticProducer.getGameType());

        playerGameStatisticProducerRepository.removeProducer(gameStatisticProducer);
    }

    @Override
    public void addGameStatisticsConsumer(final GameStatisticConsumer gameStatisticConsumer) {
        LOG.debug("updating game statistics consumer {}", gameStatisticConsumer);

        playerGameStatisticConsumerRepository.addConsumer(gameStatisticConsumer);
    }

    @Override
    public void removeGameStatisticsConsumer(final GameStatisticConsumer gameStatisticConsumer) {
        LOG.debug("updating game statistics consumer {}", gameStatisticConsumer);

        playerGameStatisticConsumerRepository.removeConsumer(gameStatisticConsumer);
    }

}
