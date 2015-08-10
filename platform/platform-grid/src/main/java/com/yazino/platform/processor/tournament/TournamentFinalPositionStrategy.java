package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TournamentAchievement;
import com.yazino.platform.model.tournament.TournamentStatisticProperty;
import com.yazino.platform.model.tournament.TournamentStatisticType;
import com.yazino.platform.repository.table.GameTypeRepository;
import com.yazino.platform.service.statistic.NewsEventPublisher;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.game.api.statistic.GameStatistics;
import com.yazino.game.api.statistic.StatisticEvent;

import static org.apache.commons.lang3.Validate.notNull;

public class TournamentFinalPositionStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(TournamentFinalPositionStrategy.class);
    private static final int MAX_NEWS_POSITION = 20;
    private static final int MAX_RANKED_POSITION = 3;
    private static final int MIN_PLAYERS_TO_RANK = 10;

    private final NewsEventPublisher newsEventPublisher;
    private final GameTypeRepository gameTypeRepository;
    private final String gameType;
    private final String newsMessage;
    private final String newsShortDescription;

    public TournamentFinalPositionStrategy(final NewsEventPublisher newsEventPublisher,
                                           final GameTypeRepository gameTypeRepository,
                                           final String gameType,
                                           final String newsMessage,
                                           final String newsShortDescription) {
        notNull(newsEventPublisher, "newsEventPublisher is null");
        notNull(gameTypeRepository, "gameTypeRepository is null");
        notNull(gameType, "gameType is null");
        notNull(newsMessage, "newsMessage is null");
        notNull(newsShortDescription, "newsShortDescription is null");

        this.newsEventPublisher = newsEventPublisher;
        this.gameTypeRepository = gameTypeRepository;
        this.gameType = gameType;
        this.newsMessage = newsMessage;
        this.newsShortDescription = newsShortDescription;
    }

    public StatisticEvent consume(final GamePlayer player,
                                  final GameStatistics statistics) {
        final GameStatistic statistic = statistics.findUniqueByName(
                TournamentStatisticType.FINAL_LEADERBOARD_POSITION.name());
        if (statistic == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Statistic %s not present. Ignoring...",
                        TournamentStatisticType.FINAL_LEADERBOARD_POSITION.name()));
            }
            return null;
        }
        final String positionStr = statistic.getProperties().get(TournamentStatisticProperty.POSITION.name());
        final String numberOfPlayersStr = statistic.getProperties().get(
                TournamentStatisticProperty.NUMBER_OF_PLAYERS.name());
        if (StringUtils.isBlank(positionStr) || StringUtils.isBlank(numberOfPlayersStr)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Statistic %s is invalid (position = %s, number of players=%s). Ignoring...",
                        TournamentStatisticType.FINAL_LEADERBOARD_POSITION.name(), positionStr, numberOfPlayersStr));
            }
            return null;
        }
        final int position = Integer.valueOf(positionStr);
        final int numberOfPlayers = Integer.valueOf(numberOfPlayersStr);

        if (position <= MAX_RANKED_POSITION && numberOfPlayers >= MIN_PLAYERS_TO_RANK) {
            return generateRankingAchievement(player, position, numberOfPlayers);
        }

        if (position <= MAX_NEWS_POSITION) {
            createPositionNewsEvent(player, position, numberOfPlayers);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Player %s - Position is irrelevant (%s). Ignoring...",
                        player.getId(), positionStr));
            }
        }

        return null;
    }

    private void createPositionNewsEvent(final GamePlayer player,
                                         final int position,
                                         final int numberOfPlayers) {
        final String imageName = gameType + "-tournament-position-" + position;
        final ParameterisedMessage news = new ParameterisedMessage(
                newsMessage, player.getName(), position, numberOfPlayers);
        final NewsEvent newsEvent = new NewsEvent.Builder(player.getId(), news)
                .setType(NewsEventType.NEWS)
                .setShortDescription(new ParameterisedMessage(newsShortDescription, position))
                .setImage(imageName)
                .build();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Player %s - Publishing news %s", player.getId(), newsEvent));
        }
        newsEventPublisher.send(newsEvent);
    }

    private StatisticEvent generateRankingAchievement(final GamePlayer player,
                                                      final int position,
                                                      final int numberOfPlayers) {
        try {
            final String achievementId = TournamentAchievement.getAchievement(
                    gameTypeRepository, gameType, position, numberOfPlayers);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Player %s - New achievement event %s", player.getId(), achievementId));
            }
            return new StatisticEvent(achievementId, 0, 1, numberOfPlayers - position);

        } catch (Exception e) {
            LOG.error("Unable to generate ranking achievement", e);
            return null;
        }
    }
}
