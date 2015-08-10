package com.yazino.platform.repository.statistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import com.yazino.game.api.statistic.GameStatisticProducer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gigaspaces.internal.utils.Assert.notNull;

@Repository("playerGameStatisticProducerRepository")
public class PlayerGameStatisticProducerRepository {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerGameStatisticProducerRepository.class);

    private final Map<String, GameStatisticProducer> gameStatisticProducers = new ConcurrentHashMap<String, GameStatisticProducer>();

    public void addProducer(final GameStatisticProducer producer) {
        notNull(producer, "producer may not be null");

        LOG.debug("Registering producer {}", producer);

        gameStatisticProducers.put(producer.getGameType(), producer);
    }

    public void removeProducer(final GameStatisticProducer producer) {
        LOG.debug("Removing producer {}", producer);

        if (producer != null) {
            gameStatisticProducers.remove(producer.getGameType());
        }
    }

    public Collection<GameStatisticProducer> getProducers() {
        return Collections.unmodifiableCollection(gameStatisticProducers.values());
    }

}
