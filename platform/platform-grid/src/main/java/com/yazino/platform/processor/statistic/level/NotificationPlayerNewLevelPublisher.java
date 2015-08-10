package com.yazino.platform.processor.statistic.level;

import com.yazino.platform.model.statistic.Notification;
import com.yazino.platform.model.statistic.NotificationMessage;
import com.yazino.platform.model.statistic.NotificationType;
import com.yazino.platform.model.statistic.PlayerLevels;
import com.yazino.platform.processor.statistic.achievement.PublishingAchievementManager;
import com.yazino.platform.service.statistic.NotificationService;
import com.yazino.platform.service.statistic.OpenGraphLevelNotifierService;
import com.yazino.platform.service.statistic.PlayerEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Component("playerNewLevelPublisher")
public class NotificationPlayerNewLevelPublisher implements PlayerNewLevelPublisher {
    private static final int DELAY = 0;

    static final Map<String, String> GAME_TYPE_NAMES = new HashMap<String, String>();

    static final String SHORT_DESCRIPTION = "You've reached level %s and been awarded a bonus of %s chips";
    static final String NEWS = "%s has reached level %s in %s! Join in the play and see how high you can reach!";
    static final String IMAGE = "playerLevel_%s_%s";

    private final NotificationService notificationService;
    private final PlayerEventService playerEventService;
    private final OpenGraphLevelNotifierService openGraphLevelNotifierService;

    static {
        GAME_TYPE_NAMES.put("TEXAS_HOLDEM", "Texas Holdem");
        GAME_TYPE_NAMES.put("BLACKJACK", "Blackjack");
        GAME_TYPE_NAMES.put("ROULETTE", "Roulette");
        GAME_TYPE_NAMES.put("SLOTS", "Slots");
        GAME_TYPE_NAMES.put("HISSTERIA", "Hissteria");
        GAME_TYPE_NAMES.put("BINGO", "Extreme Bingo");
        GAME_TYPE_NAMES.put("HIGH_STAKES", "High Stakes");
    }

    @Autowired
    public NotificationPlayerNewLevelPublisher(
            @Qualifier("notificationService") final NotificationService notificationService,
            @Qualifier("playerEventService") final PlayerEventService playerEventService,
            final OpenGraphLevelNotifierService openGraphLevelNotifierService) {
        notNull(notificationService, "newsEventPublisher is null");
        notNull(playerEventService, "playerEventService is null");
        notNull(openGraphLevelNotifierService, "openGraphLevelNotifierService is null");
        this.notificationService = notificationService;
        this.playerEventService = playerEventService;
        this.openGraphLevelNotifierService = openGraphLevelNotifierService;

    }

    @Override
    public void publishNewLevel(final PlayerLevels player,
                                final String gameType,
                                final BigDecimal chipBonus) {
        notNull(player, "player is null");
        notNull(gameType, "gameType is null");
        notNull(chipBonus, "chipBonus is null");
        final int playerLevel = player.retrieveLevel(gameType);
        publishNewLevelEvent(player, gameType, chipBonus, playerLevel);
        publishNotification(player, gameType, chipBonus, playerLevel);
    }

    private void publishNewLevelEvent(final PlayerLevels player,
                                      final String gameType,
                                      final BigDecimal chipBonus,
                                      final int playerLevel) {
        playerEventService.publishNewLevel(player.getPlayerId(), gameType, playerLevel, chipBonus);
        openGraphLevelNotifierService.publishNewLevel(player.getPlayerId(), gameType, playerLevel);
    }

    private void publishNotification(final PlayerLevels player,
                                     final String gameType,
                                     final BigDecimal chipBonus,
                                     final int playerLevel) {
        final NotificationMessage news = new NotificationMessage(NEWS
                , PublishingAchievementManager.PLAYER_NAME_HOLDER
                , playerLevel
                , GAME_TYPE_NAMES.get(gameType));
        final Notification notification = new Notification.Builder(player.getPlayerId(), news)
                .setType(NotificationType.LEVEL)
                .setShortDescription(new NotificationMessage(SHORT_DESCRIPTION, playerLevel, chipBonus))
                .setImage(String.format(IMAGE, gameType, playerLevel))
                .setDelay(DELAY)
                .build();
        notificationService.publish(notification);
    }

}
