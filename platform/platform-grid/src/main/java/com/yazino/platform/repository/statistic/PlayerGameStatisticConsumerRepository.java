package com.yazino.platform.repository.statistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import com.yazino.game.api.statistic.GameStatisticConsumer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gigaspaces.internal.utils.Assert.notNull;

@Repository("playerGameStatisticConsumerRepository")
public class PlayerGameStatisticConsumerRepository {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerGameStatisticConsumerRepository.class);

    private final Map<Class<?>, GameStatisticConsumer> gameStatisticConsumers = new ConcurrentHashMap<Class<?>, GameStatisticConsumer>();

    public void addConsumer(final GameStatisticConsumer consumer) {
        notNull(consumer, "consumer may not be null");

        LOG.debug("Registering consumer {}", consumer);

        gameStatisticConsumers.put(consumer.getClass(), consumer);
    }

    public void removeConsumer(final GameStatisticConsumer consumer) {
        LOG.debug("Removing consumer {}", consumer);

        if (consumer != null) {
            gameStatisticConsumers.remove(consumer.getClass());
        }
    }

    public Collection<GameStatisticConsumer> getConsumers() {
        return Collections.unmodifiableCollection(gameStatisticConsumers.values());
    }

}
