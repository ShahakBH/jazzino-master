package com.yazino.platform.processor.statistic.level;

import com.yazino.platform.model.statistic.*;
import com.yazino.platform.service.statistic.NotificationService;
import com.yazino.platform.service.statistic.OpenGraphLevelNotifierService;
import com.yazino.platform.service.statistic.PlayerEventService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class NotificationPlayerNewLevelPublisherTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PlayerEventService playerEventService;

    @Mock
    private OpenGraphLevelNotifierService openGraphLevelNotifierService;

    private PlayerNewLevelPublisher underTest;


    @Before
    public void setUp() {
        underTest = new NotificationPlayerNewLevelPublisher(notificationService, playerEventService, openGraphLevelNotifierService);
    }

    @Test
    public void shouldPublishNewLevelEvent() {
        String gameType = "SLOTS";
        PlayerLevel playerLevel = new PlayerLevel(1, BigDecimal.ONE);
        BigDecimal playerId = BigDecimal.ONE;
        Map<String, PlayerLevel> levels = new HashMap<String, PlayerLevel>();
        levels.put(gameType, playerLevel);
        PlayerLevels playerLevels = new PlayerLevels(playerId, levels);

        BigDecimal chipBonus = new BigDecimal("11111");
        underTest.publishNewLevel(playerLevels, gameType, chipBonus);

        verify(playerEventService, times(1)).publishNewLevel(playerId, gameType, playerLevel.getLevel(), chipBonus);
        verify(openGraphLevelNotifierService, times(1)).publishNewLevel(playerId, gameType, playerLevel.getLevel());
    }

    @Test
    public void shouldPublishNotification() {
        String gameType = "TEXAS_HOLDEM";
        PlayerLevel playerLevel = new PlayerLevel(1, BigDecimal.ONE);
        BigDecimal playerId = BigDecimal.ONE;
        Map<String, PlayerLevel> levels = new HashMap<String, PlayerLevel>();
        levels.put(gameType, playerLevel);
        PlayerLevels playerLevels = new PlayerLevels(playerId, levels);

        BigDecimal chipBonus = new BigDecimal("11111");
        underTest.publishNewLevel(playerLevels, gameType, chipBonus);

        NotificationMessage news = new NotificationMessage(NotificationPlayerNewLevelPublisher.NEWS
                , "${name}"
                , playerLevel.getLevel()
                , NotificationPlayerNewLevelPublisher.GAME_TYPE_NAMES.get(gameType));

        Notification notification = new Notification.Builder(playerLevels.getPlayerId(), news)
                .setType(NotificationType.LEVEL)
                .setShortDescription(new NotificationMessage(NotificationPlayerNewLevelPublisher.SHORT_DESCRIPTION, playerLevel.getLevel(), chipBonus))
                .setImage(String.format(NotificationPlayerNewLevelPublisher.IMAGE, gameType, playerLevel.getLevel()))
                .setDelay(0)
                .build();

        verify(notificationService, times(1)).publish(notification);
    }

    @Test
    public void shouldHaveCorrectMessage() {
        String gameType = "TEXAS_HOLDEM";
        PlayerLevel playerLevel = new PlayerLevel(1, BigDecimal.ONE);
        BigDecimal playerId = BigDecimal.ONE;
        Map<String, PlayerLevel> levels = new HashMap<String, PlayerLevel>();
        levels.put(gameType, playerLevel);
        PlayerLevels playerLevels = new PlayerLevels(playerId, levels);

        BigDecimal chipBonus = new BigDecimal("11111");
        underTest.publishNewLevel(playerLevels, gameType, chipBonus);

        ArgumentCaptor<Notification> argument = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService).publish(argument.capture());

        Notification notification = argument.getValue();
        assertEquals("${name} has reached level 1 in Texas Holdem! Join in the play and see how high you can reach!", notification.getNews().getMessage());
    }
}
