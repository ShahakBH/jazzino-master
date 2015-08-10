package com.yazino.platform.gamehost;

import com.yazino.platform.model.table.GameCompleted;
import com.yazino.platform.processor.table.GameCompletePublisher;
import com.yazino.platform.repository.table.GameRepository;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameStatus;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class GigaspaceGameCompletePublisher implements GameCompletePublisher {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceGameCompletePublisher.class);

    private final GigaSpace gigaSpace;
    private final GameRepository gameRepository;

    /*
     CGLib Constructor
      */
    protected GigaspaceGameCompletePublisher() {
        this.gigaSpace = null;
        this.gameRepository = null;
    }

    @Autowired
    public GigaspaceGameCompletePublisher(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                          final GameRepository gameRepository) {
        notNull(gigaSpace, "gigaSpace is null");
        notNull(gameRepository, "gameRepository is null");

        this.gigaSpace = gigaSpace;
        this.gameRepository = gameRepository;
    }

    public void publishCompletedGame(final GameStatus gameStatus,
                                     final String gameType,
                                     final BigDecimal tableId,
                                     final String clientId) {
        notNull(gigaSpace, "Invalid constructor used Gigaspace required");
        notNull(gameStatus, "gameStatus is null");
        notBlank(gameType, "gameType is blank");
        notNull(tableId, "tableId is null");
        if (LOG.isDebugEnabled()) {
            LOG.debug("publishCompletedGame " + ReflectionToStringBuilder.reflectionToString(gameStatus));
        }
        final GameRules gameRules = gameRepository.getGameRules(gameType);
        if (!gameRules.isComplete(gameStatus)) {
            LOG.error("GameStatus is not complete - not publishing"
                    + ReflectionToStringBuilder.reflectionToString(gameStatus));
            return;
        }
        gigaSpace.write(new GameCompleted(gameStatus, gameType, tableId, clientId));
    }
}
