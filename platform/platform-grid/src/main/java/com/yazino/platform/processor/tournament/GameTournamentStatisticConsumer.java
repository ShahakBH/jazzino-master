package com.yazino.platform.processor.tournament;


import com.yazino.game.api.*;
import com.yazino.platform.repository.table.GameTypeRepository;
import com.yazino.platform.service.statistic.NewsEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.statistic.GameStatisticConsumer;
import com.yazino.game.api.statistic.GameStatistics;
import com.yazino.game.api.statistic.StatisticEvent;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.commons.lang3.Validate.notNull;

public class GameTournamentStatisticConsumer implements GameStatisticConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(GameTournamentStatisticConsumer.class);

    private final Map<String, TournamentFinalPositionStrategy> strategies
            = new HashMap<String, TournamentFinalPositionStrategy>();
    private final ReadWriteLock strategyLock = new ReentrantReadWriteLock();
    private final NewsEventPublisher newsEventPublisher;
    private final GameTypeRepository gameTypeRepository;

    @Autowired(required = true)
    public GameTournamentStatisticConsumer(final NewsEventPublisher newsEventPublisher,
                                           final GameTypeRepository gameTypeRepository) {
        notNull(newsEventPublisher, "newsEventPublisher is null");
        notNull(gameTypeRepository, "gameTypeRepository is null");

        this.newsEventPublisher = newsEventPublisher;
        this.gameTypeRepository = gameTypeRepository;
    }

    public Set<StatisticEvent> consume(final GamePlayer player,
                                       final BigDecimal tableId,
                                       final String gameType,
                                       final Map<String, String> clientProperties,
                                       final GameStatistics statistics) {
        final TournamentFinalPositionStrategy strategy = forGameType(gameType);
        if (strategy == null) {
            throw new IllegalArgumentException("Cannot handle game type: " + gameType);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Consuming for game type: %s; player %s; table %s; client props: %s; stats: %s",
                    gameType, player, tableId, clientProperties, statistics));
        }

        final StatisticEvent event = strategy.consume(player, statistics);
        if (event != null) {
            return new HashSet<StatisticEvent>(Arrays.asList(event));
        }
        return Collections.emptySet();
    }

    public boolean acceptsGameType(final String gameTypeId) {
        if (forGameType(gameTypeId) != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Handler is already cached for game type " + gameTypeId);
            }

            return true;
        }

        final GameType gameType = gameTypeRepository.getGameType(gameTypeId);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Testing game type for ID " + gameTypeId + ": " + gameType);
        }

        return gameType != null && gameType.isSupported(GameFeature.TOURNAMENT);
    }

    private TournamentFinalPositionStrategy createStrategyForGameType(final GameType gameType) {
        final GameMetaData gameMetaData = gameTypeRepository.getMetaDataFor(gameType.getId());
        if (gameMetaData == null) {
            throw new IllegalStateException("Cannot fetch meta-data for " + gameType.getId());
        }

        strategyLock.writeLock().lock();
        try {
            final TournamentFinalPositionStrategy strategy = initialiseStrategy(gameType.getId(),
                    gameMetaData.forKey(GameMetaDataKey.TOURNAMENT_RANKING_MESSAGE),
                    gameMetaData.forKey(GameMetaDataKey.TOURNAMENT_SUMMARY_MESSAGE));
            strategies.put(gameType.getId(), strategy);
            return strategy;

        } finally {
            strategyLock.writeLock().unlock();
        }
    }

    TournamentFinalPositionStrategy initialiseStrategy(final String gameTypeId,
                                                       final String tournamentRankingMessage,
                                                       final String tournamentRankingShortMessage) {
        return new TournamentFinalPositionStrategy(newsEventPublisher, gameTypeRepository, gameTypeId,
                tournamentRankingMessage, tournamentRankingShortMessage);
    }

    private TournamentFinalPositionStrategy forGameType(final String gameTypeId) {
        strategyLock.readLock().lock();

        final GameType gameType;
        try {
            for (String strategyGameTypeId : strategies.keySet()) {
                if (strategyGameTypeId.equals(gameTypeId)) {
                    return strategies.get(gameTypeId);
                }
            }

            gameType = gameTypeRepository.getGameType(gameTypeId);

        } finally {
            strategyLock.readLock().unlock();
        }

        if (gameType != null && gameType.isSupported(GameFeature.TOURNAMENT)) {
            return createStrategyForGameType(gameType);
        }

        return null;
    }
}
