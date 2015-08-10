package com.yazino.platform.service.statistic;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.event.message.PlayerLevelEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.playerevent.PlayerEvent;
import com.yazino.platform.playerevent.PlayerEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Component("localPlayerEventService")
@Qualifier("delegatePlayerEventService")
public class LocalPlayerEventService implements PlayerEventService {
    private static final Logger LOG = LoggerFactory.getLogger(LocalPlayerEventService.class);

    private final PlayerService playerService;
    private final QueuePublishingService<PlayerLevelEvent> playerLevelEventService;

    @Autowired
    public LocalPlayerEventService(final PlayerService playerService,
                                   @Qualifier("playerLevelEventQueuePublishingService")
                                   final QueuePublishingService<PlayerLevelEvent> playerLevelEventService) {
        notNull(playerService, "playerService is null");
        notNull(playerLevelEventService, "playerLevelEventService is null");

        this.playerService = playerService;
        this.playerLevelEventService = playerLevelEventService;
    }

    @Override
    public void publishNewLevel(final BigDecimal playerId,
                                final String gameType,
                                final int level,
                                final BigDecimal bonusAmount) {
        notNull(playerId, "playerId is null");
        notNull(gameType, "gameType is null");
        notNull(bonusAmount, "bonusAmount is null");

        final PlayerEvent playerEvent = new PlayerEvent(playerId, PlayerEventType.NEW_LEVEL,
                gameType, String.valueOf(level), String.valueOf(bonusAmount));
        LOG.debug("Processing event {} ", playerEvent);

        awardChipsForLevel(playerId, gameType, level, bonusAmount);

        sendLevelEvent(playerId, gameType, level);
    }

    private void sendLevelEvent(final BigDecimal playerId,
                                final String gameType,
                                final int level) {
        playerLevelEventService.send(new PlayerLevelEvent(playerId.toPlainString(), gameType, Integer.toString(level)));
    }

    private void awardChipsForLevel(final BigDecimal playerId,
                                    final String gameType,
                                    final int level,
                                    final BigDecimal bonusAmount) {
        try {
            playerService.postTransaction(playerId, null, bonusAmount, "Level Bonus", gameType + " level " + level);

        } catch (WalletServiceException e) {
            LOG.error("Could not award chips for player's new level (playerId={},gameType={},level={},bonusAmount={})",
                    playerId, gameType, level, bonusAmount, e);
        }
    }
}
