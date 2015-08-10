package com.yazino.platform.processor.statistic.achievement;

import com.yazino.platform.model.statistic.*;
import com.yazino.platform.service.statistic.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Component("achievementManager")
public class PublishingAchievementManager implements AchievementManager {
    private static final Logger LOG = LoggerFactory.getLogger(PublishingAchievementManager.class);
    public static final String PLAYER_NAME_HOLDER = "${name}";

    private enum GameType {
        BLACKJACK("Blackjack"),
        TEXAS_HOLDEM("Texas Hold'em"),
        ROULETTE("Roulette"),
        SLOTS("Slots"),
        HIGH_STAKES("High Stakes"),
        HISSTERIA("Hissteria"),
        BINGO("Bingo");

        private final String displayName;

        private GameType(final String displayName) {
            notNull(displayName, "displayName may not be null");

            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final NotificationService notificationService;

    @Autowired(required = true)
    public PublishingAchievementManager(
            @Qualifier("notificationService") final NotificationService notificationService) {
        notNull(notificationService, "notificationService is null");
        this.notificationService = notificationService;
    }

    @Override
    public void awardAchievement(final PlayerAchievements player,
                                 final Achievement achievement,
                                 final List<Object> parameters,
                                 final long delay) {
        notNull(player, "Player may not be null");
        notNull(achievement, "Achievement may not be null");
        if (!player.hasAchievement(achievement.getId())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Player %s - Awarding %s ", player.getPlayerId(), achievement.getId()));
            }
            player.awardAchievement(achievement.getId());
        }
        createAndPublishNotification(player, achievement, parameters, delay);
    }

    private void createAndPublishNotification(final PlayerAchievements player,
                                              final Achievement achievement,
                                              final List<Object> achievementParameters,
                                              final long delay) {
        final Notification notification = createNotification(player, achievement, achievementParameters, delay);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Sending notification to player [%s]: %s",
                    player.getPlayerId(), notification));
        }
        notificationService.publish(notification);
    }

    private Notification createNotification(final PlayerAchievements player,
                                            final Achievement achievement,
                                            final List<Object> achievementParameters, final long delay) {
        final Object[] messageParameters = buildMessageParameters(achievementParameters, PLAYER_NAME_HOLDER);
        final NotificationMessage message = new NotificationMessage(achievement.getMessage(), messageParameters);
        final Notification.Builder builder = new Notification.Builder(player.getPlayerId(), message)
                .setType(NotificationType.ACHIEVEMENT)
                .setTitle(achievement.getTitle())
                .setImage(achievement.getId())
                .setGameType(achievement.getGameType())
                .setDelay(delay);
        addShortDescription(builder, achievement, achievementParameters);
        addPostingInformation(builder, achievement, player);
        return builder.build();
    }

    private Object[] buildMessageParameters(final List<Object> achievementParameters, final String playerName) {
        if (achievementParameters == null) {
            return new Object[]{playerName};
        }
        final Object[] messageParameters = new Object[achievementParameters.size() + 1];
        messageParameters[0] = playerName;
        System.arraycopy(achievementParameters.toArray(), 0, messageParameters, 1, achievementParameters.size());
        return messageParameters;
    }

    private void addShortDescription(final Notification.Builder builder,
                                     final Achievement achievement,
                                     final List<Object> achievementParameters) {
        final Object[] shortDescriptionParameters = buildShortMessageParameters(achievementParameters);
        final NotificationMessage shortDescription = new NotificationMessage(achievement.getShortDescription(),
                shortDescriptionParameters);
        builder.setShortDescription(shortDescription);
    }

    private Object[] buildShortMessageParameters(final List<Object> achievementParameters) {
        if (achievementParameters == null) {
            return new Object[0];
        }
        final Object[] shortMessageParameters = new Object[achievementParameters.size()];
        System.arraycopy(achievementParameters.toArray(), 0, shortMessageParameters, 0, achievementParameters.size());
        return shortMessageParameters;
    }

    private void addPostingInformation(final Notification.Builder builder,
                                       final Achievement achievement,
                                       final PlayerAchievements player) {
        final String displayName = GameType.valueOf(achievement.getGameType()).getDisplayName();
        final Object[] postedAchievementParameters = new Object[]{PLAYER_NAME_HOLDER,
                achievement.getTitle(),
                displayName,
                achievement.getGameType()};
        final String postedAchievementTitleText = buildMessage(achievement.getPostedAchievementTitleText(),
                postedAchievementParameters);
        final String postedAchievementTitleLink = buildMessage(achievement.getPostedAchievementTitleLink(),
                postedAchievementParameters);
        final String postedAchievementActionText = buildMessage(achievement.getPostedAchievementActionText(),
                postedAchievementParameters);
        final String postedAchievementActionLink = buildMessage(achievement.getPostedAchievementActionLink(),
                postedAchievementParameters);
        builder.setPostedAchievementTitleText(postedAchievementTitleText)
                .setPostedAchievementTitleLink(postedAchievementTitleLink)
                .setPostedAchievementActionText(postedAchievementActionText)
                .setPostedAchievementActionLink(postedAchievementActionLink);
    }

    private String buildMessage(final String achievementMessage, final Object[] parameters) {
        return new NotificationMessage(achievementMessage, parameters).getMessage();
    }
}
