package com.yazino.platform.processor.statistic.level;

import com.yazino.platform.messaging.dispatcher.PlayerDocumentDispatcher;
import com.yazino.platform.model.statistic.LevelDefinition;
import com.yazino.platform.model.statistic.LevelingSystem;
import com.yazino.platform.model.statistic.PlayerLevel;
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

@Component("playerXPPublisher")
public class DocumentPlayerXPPublisher extends ThreadPoolExecutor implements PlayerXPPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentPlayerXPPublisher.class);
    private static final long KEEP_ALIVE = 30;
    public static final String DOCUMENT_TYPE = "PLAYER_XP";

    private final PlayerDocumentDispatcher dispatcher;
    private final PlayerXPMessageFactory messageFactory;

    @Autowired
    public DocumentPlayerXPPublisher(
            @Value("${strata.worker.playerstats.xp-publishing.core-threads}") final int corePoolSize,
            @Value("${strata.worker.playerstats.xp-publishing.max-threads}") final int maxPoolSize,
            @Qualifier("playerDocumentDispatcher") final PlayerDocumentDispatcher dispatcher,
            @Qualifier("playerXPMessageFactory") final PlayerXPMessageFactory messageFactory) {
        super(corePoolSize, maxPoolSize, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        notNull(dispatcher, "dispatcher is null");
        notNull(messageFactory, "messageFactory is null");
        this.dispatcher = dispatcher;
        this.messageFactory = messageFactory;
    }

    @Override
    public void publish(final BigDecimal playerId,
                        final String gameType,
                        final PlayerLevel playerLevel,
                        final LevelingSystem levelingSystem) {
        submit(new PublishXPTask(playerId, gameType, playerLevel, levelingSystem));

    }

    private class PublishXPTask implements Runnable {
        private final BigDecimal playerId;
        private final String gameType;
        private final PlayerLevel playerLevel;
        private final LevelingSystem levelingSystem;

        public PublishXPTask(final BigDecimal playerId,
                             final String gameType,
                             final PlayerLevel playerLevel,
                             final LevelingSystem levelingSystem) {
            this.playerId = playerId;
            this.gameType = gameType;
            this.playerLevel = playerLevel;
            this.levelingSystem = levelingSystem;
        }

        @Override
        public void run() {
            final LevelDefinition definition = levelingSystem.retrieveLevelDefinition(playerLevel.getLevel());
            final String json = messageFactory.create(gameType, playerLevel, definition);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Publishing %s doc for player %s: %s", DOCUMENT_TYPE, playerId, json));
            }
            dispatcher.dispatch(playerId, DOCUMENT_TYPE, json);
        }
    }

}
