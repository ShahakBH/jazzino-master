package com.yazino.platform.service.statistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.Validate.notNull;

@Component("playerEventService")
public class BackgroundPlayerEventService extends ThreadPoolExecutor implements PlayerEventService {
    private static final Logger LOG = LoggerFactory.getLogger(BackgroundPlayerEventService.class);
    public static final int KEEP_ALIVE = 60;
    private final PlayerEventService delegate;

    @Autowired
    public BackgroundPlayerEventService(@Value("${strata.worker.playerevents.core-threads}") final int corePoolSize,
                                        @Value("${strata.worker.playerevents.max-threads}") final int maxPoolSize,
                                        @Qualifier("delegatePlayerEventService") final PlayerEventService delegate) {
        super(corePoolSize, maxPoolSize, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        notNull(delegate, "dispatcher is null");
        this.delegate = delegate;
    }

    @Override
    public void publishNewLevel(final BigDecimal playerId,
                                final String gameType,
                                final int level,
                                final BigDecimal bonusAmount) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Queueing task to publish player event ");
        }
        this.execute(new PublishTask(playerId, gameType, level, bonusAmount));
    }

    private class PublishTask implements Runnable {
        private final BigDecimal playerId;
        private final String gameType;
        private final int level;
        private final BigDecimal bonusAmount;

        public PublishTask(final BigDecimal playerId,
                           final String gameType,
                           final int level,
                           final BigDecimal bonusAmount) {
            this.playerId = playerId;
            this.gameType = gameType;
            this.level = level;
            this.bonusAmount = bonusAmount;
        }

        @Override
        public void run() {
            delegate.publishNewLevel(playerId, gameType, level, bonusAmount);
        }
    }
}
