package com.yazino.platform.model.statistic;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class NotificationTest {

    public static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    public static final NotificationMessage NEWS = new NotificationMessage("news");

    @Test
    public void shouldBuildWithMinimumData() {
        final Notification underTest = new Notification.Builder(PLAYER_ID, new NotificationMessage("news")).build();
        assertEquals(NotificationType.UNKNOWN, underTest.getType());
        assertEquals(Notification.DEFAULT_ACTION_TEXT, underTest.getPostedAchievementActionText());
        assertEquals(Notification.DEFAULT_ACTION_TEXT, underTest.getPostedAchievementTitleText());
        assertEquals(Notification.DEFAULT_ACTION_LINK, underTest.getPostedAchievementActionLink());
        assertEquals(Notification.DEFAULT_ACTION_LINK, underTest.getPostedAchievementTitleLink());
    }

    @Test
    public void shouldNotReplaceDefaultActionsWithEmptyMessages() {
        final Notification expected = new Notification.Builder(PLAYER_ID, NEWS).build();
        final Notification withEmpties = new Notification.Builder(PLAYER_ID, NEWS)
                .setPostedAchievementActionText("")
                .setPostedAchievementActionLink("")
                .setPostedAchievementTitleText("")
                .setPostedAchievementActionLink("")
                .build();
        final Notification withNulls = new Notification.Builder(PLAYER_ID, NEWS)
                .setPostedAchievementActionText(null)
                .setPostedAchievementActionLink(null)
                .setPostedAchievementTitleText(null)
                .setPostedAchievementActionLink(null)
                .build();
        assertEquals(expected, withEmpties);
        assertEquals(expected, withNulls);
    }
}
