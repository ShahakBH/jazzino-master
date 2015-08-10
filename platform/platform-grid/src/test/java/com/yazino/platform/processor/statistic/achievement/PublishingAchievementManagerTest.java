package com.yazino.platform.processor.statistic.achievement;

import com.yazino.platform.model.statistic.*;
import com.yazino.platform.service.statistic.NotificationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.Mockito.*;

public class PublishingAchievementManagerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private static final String PLAYER_NAME = "${name}";
    public static final String GAME_TYPE = "BLACKJACK";
    private Achievement achievement;

    private NotificationService notificationService;
    private PlayerAchievements player;

    private PublishingAchievementManager unit;

    @Before
    public void setUp() throws Exception {
        achievement = new Achievement("anAchievement",
                1,
                "aTitle",
                "aMessage %1$s",
                "aShortDesc",
                "how to get 1",
                null,
                null,
                null,
                null,
                GAME_TYPE,
                set("anEvent"),
                "anAccumulator",
                null,
                true);

        notificationService = mock(NotificationService.class);
        player = mock(PlayerAchievements.class);

        unit = new PublishingAchievementManager(notificationService);

        when(player.getPlayerId()).thenReturn(PLAYER_ID);
    }

    @Test
    public void shouldAwardAchievement() {
        unit.awardAchievement(player, achievement, null, 0);
        verify(player).awardAchievement(achievement.getId());
    }

    @Test
    public void shouldNotAwardAchievementIfPlayerAlreadyHasIt() {
        when(player.hasAchievement(achievement.getId())).thenReturn(true);
        unit.awardAchievement(player, achievement, null, 0);
        verify(player, never()).awardAchievement(anyString());
    }

    @Test
    public void shouldPublishNotification() {
        unit.awardAchievement(player, achievement, null, 0);
        final NotificationMessage notificationMessage = new NotificationMessage(achievement.getMessage(), PLAYER_NAME);
        final Notification notification = new Notification.Builder(PLAYER_ID, notificationMessage)
                .setType(NotificationType.ACHIEVEMENT)
                .setTitle(achievement.getTitle())
                .setShortDescription(new NotificationMessage(achievement.getShortDescription()))
                .setImage(achievement.getId())
                .setGameType(achievement.getGameType())
                .build();
        verify(notificationService).publish(notification);
    }

    @Test
    public void shouldPublishNotificationForAchievementWithTitleAndAction() {
        achievement.setPostedAchievementTitleText("%s got %s on %s (%s)");
        achievement.setPostedAchievementTitleLink("title_link/%4$s");
        achievement.setPostedAchievementActionText("challenge %s");
        achievement.setPostedAchievementActionLink("action_link/%4$s");
        unit.awardAchievement(player, achievement, null, 0);
        final NotificationMessage notificationMessage = new NotificationMessage(achievement.getMessage(), PLAYER_NAME);
        final Notification notification = new Notification.Builder(PLAYER_ID, notificationMessage)
                .setType(NotificationType.ACHIEVEMENT)
                .setTitle(achievement.getTitle())
                .setShortDescription(new NotificationMessage(achievement.getShortDescription()))
                .setPostedAchievementTitleText("${name} got aTitle on Blackjack (BLACKJACK)")
                .setPostedAchievementTitleLink("title_link/BLACKJACK")
                .setPostedAchievementActionText("challenge ${name}")
                .setPostedAchievementActionLink("action_link/BLACKJACK")
                .setImage(achievement.getId())
                .setGameType(achievement.getGameType())
                .build();
        verify(notificationService).publish(notification);
    }

    @Test
    public void shouldPublishNotificationUsingAchievementParameters() {
        ReflectionTestUtils.setField(achievement, "message", "Player %s has %s and %s");
        achievement.setShortDescription("You got %s and %s");
        final List<Object> parameters = new ArrayList<Object>(Arrays.asList("P1", "P2"));
        unit.awardAchievement(player, achievement, parameters, 0);
        final NotificationMessage notificationMessage = new NotificationMessage("Player ${name} has P1 and P2");
        final Notification notification = new Notification.Builder(PLAYER_ID, notificationMessage)
                .setType(NotificationType.ACHIEVEMENT)
                .setTitle(achievement.getTitle())
                .setShortDescription(new NotificationMessage("You got P1 and P2"))
                .setImage(achievement.getId())
                .setGameType(achievement.getGameType())
                .build();
        verify(notificationService).publish(notification);
    }

    @Test
    public void aNewsEventIsProvidedWithAnyPassedParameters() {
        final List<Object> params = Arrays.asList((Object) "p1", "p2");
        final int delay = 750;
        unit.awardAchievement(player, achievement, params, delay);
        final NotificationMessage notificationMessage = new NotificationMessage(achievement.getMessage(),
                PLAYER_NAME,
                "p1",
                "p2");
        final Notification notification = new Notification.Builder(PLAYER_ID, notificationMessage)
                .setType(NotificationType.ACHIEVEMENT)
                .setTitle(achievement.getTitle())
                .setShortDescription(new NotificationMessage(achievement.getShortDescription(), "p1", "p2"))
                .setImage(achievement.getId())
                .setDelay(delay)
                .setGameType(achievement.getGameType()).build();
        verify(notificationService).publish(notification);
    }

    @Test
    public void aNewsEventIsProvidedWithZeroParameters() {
        final List<Object> params = Collections.emptyList();
        final int delay = 750;
        unit.awardAchievement(player, achievement, params, delay);
        final NotificationMessage notificationMessage = new NotificationMessage(achievement.getMessage(), PLAYER_NAME);
        final Notification notification = new Notification.Builder(PLAYER_ID, notificationMessage)
                .setType(NotificationType.ACHIEVEMENT)
                .setTitle(achievement.getTitle())
                .setShortDescription(new NotificationMessage(achievement.getShortDescription()))
                .setImage(achievement.getId())
                .setDelay(delay)
                .setGameType(achievement.getGameType())
                .build();
        verify(notificationService).publish(notification);
    }

    private static <T> Set<T> set(final T... items) {
        return new HashSet<T>(Arrays.asList(items));
    }
}
