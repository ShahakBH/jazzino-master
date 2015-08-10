package com.yazino.platform.service.statistic;

import com.yazino.platform.model.statistic.*;
import com.yazino.platform.playerstatistic.service.AchievementDetails;
import com.yazino.platform.playerstatistic.service.AchievementInfo;
import com.yazino.platform.playerstatistic.service.LevelInfo;
import com.yazino.platform.processor.statistic.level.PlayerXPPublisher;
import com.yazino.platform.repository.statistic.AchievementRepository;
import com.yazino.platform.repository.statistic.LevelingSystemRepository;
import com.yazino.platform.repository.statistic.PlayerAchievementsRepository;
import com.yazino.platform.repository.statistic.PlayerLevelsRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceRemotingPlayerStatsServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(1034);
    private static final int PLAYER_LEVEL = 3;
    private static final long PLAYER_XP = 900;
    private static final int NEXT_LEVEL_XP = 1000;
    private static final int THIS_LEVEL_XP = 100;

    @Mock
    private PlayerLevelsRepository playerLevelsRepository;
    @Mock
    private LevelingSystemRepository levelingSystemRepository;
    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private PlayerAchievementsRepository playerAchievementsRepository;
    @Mock
    private PlayerXPPublisher playerXPPublisher;

    private GigaspaceRemotingPlayerStatsService underTest;

    @Before
    public void setUp() {
        underTest = new GigaspaceRemotingPlayerStatsService(playerLevelsRepository, levelingSystemRepository,
                achievementRepository, playerAchievementsRepository, playerXPPublisher);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullPlayerLevelsRepository() {
        new GigaspaceRemotingPlayerStatsService(null, levelingSystemRepository,
                achievementRepository, playerAchievementsRepository, playerXPPublisher);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullLevelingSystemRepository() {
        new GigaspaceRemotingPlayerStatsService(playerLevelsRepository, null,
                achievementRepository, playerAchievementsRepository, playerXPPublisher);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullAchievementRepository() {
        new GigaspaceRemotingPlayerStatsService(playerLevelsRepository, levelingSystemRepository,
                null, playerAchievementsRepository, playerXPPublisher);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullPlayerAchivementsRepository() {
        new GigaspaceRemotingPlayerStatsService(playerLevelsRepository, levelingSystemRepository,
                achievementRepository, null, playerXPPublisher);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullPlayerXPPublisherRepository() {
        new GigaspaceRemotingPlayerStatsService(playerLevelsRepository, levelingSystemRepository,
                achievementRepository, playerAchievementsRepository, null);
    }

    @Test(expected = NullPointerException.class)
    public void publishExperienceThrowsAnExceptionForANullPlayerId() {
        underTest.publishExperience(null, "aGameType");
    }

    @Test(expected = NullPointerException.class)
    public void publishExperienceThrowsAnExceptionForANullGameType() {
        underTest.publishExperience(PLAYER_ID, null);
    }

    @Test
    public void publishExperiencePublishesToTheXPPublisher() {
        when(playerLevelsRepository.forPlayer(PLAYER_ID)).thenReturn(aPlayerLevels());
        when(levelingSystemRepository.findByGameType("aGameType")).thenReturn(aLevelingSystem());

        underTest.publishExperience(PLAYER_ID, "aGameType");

        verify(playerXPPublisher).publish(PLAYER_ID, "aGameType", aPlayerLevel(), aLevelingSystem());
    }

    @Test(expected = NullPointerException.class)
    public void getLevelThrowsAnExceptionForANullPlayerId() {
        underTest.getLevel(null, "aGameType");
    }

    @Test(expected = NullPointerException.class)
    public void getLevelThrowsAnExceptionForANullGameType() {
        underTest.getLevel(PLAYER_ID, null);
    }

    @Test
    public void getLevelRetrievesTheLevelFromTheRepository() {
        when(playerLevelsRepository.forPlayer(PLAYER_ID)).thenReturn(aPlayerLevels());

        assertThat(underTest.getLevel(PLAYER_ID, "aGameType"), is(equalTo(PLAYER_LEVEL)));
    }

    @Test(expected = NullPointerException.class)
    public void getLevelInfoThrowsAnExceptionForANullPlayerId() {
        underTest.getLevelInfo(null, "aGameType");
    }

    @Test(expected = NullPointerException.class)
    public void getLevelInfoThrowsAnExceptionForANullGameType() {
        underTest.getLevelInfo(PLAYER_ID, null);
    }

    @Test
    public void getLevelInfoReturnsCurrentLevelInformationForThePlayerAndGameType() {
        when(playerLevelsRepository.forPlayer(PLAYER_ID)).thenReturn(aPlayerLevels());
        when(levelingSystemRepository.findByGameType("aGameType")).thenReturn(aLevelingSystem());

        assertThat(underTest.getLevelInfo(PLAYER_ID, "aGameType"),
                is(equalTo(new LevelInfo(PLAYER_LEVEL, PLAYER_XP - THIS_LEVEL_XP, NEXT_LEVEL_XP - THIS_LEVEL_XP))));
    }

    @Test
    public void getLevelInfoReturnsNullIfNoLevelingSystemExistsForGameType() {
        when(playerLevelsRepository.forPlayer(PLAYER_ID)).thenReturn(aPlayerLevels());

        assertThat(underTest.getLevelInfo(PLAYER_ID, "aGameType"), is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void getAchievementInfoThrowsAnExceptionForANullPlayerId() {
        underTest.getAchievementInfo(null, "aGameType");
    }

    @Test(expected = NullPointerException.class)
    public void getAchievementInfoThrowsAnExceptionForANullGameType() {
        underTest.getAchievementInfo(PLAYER_ID, null);
    }

    @Test
    public void getAchievementInfoReturnsTheCountOfAchievementsForThePlayerAndGameType() {
        when(achievementRepository.findByGameType("aGameType"))
                .thenReturn(newArrayList(anAchievement("one"), anAchievement("two"), anAchievement("three")));
        when(playerAchievementsRepository.forPlayer(PLAYER_ID)).thenReturn(aPlayerAchievements());

        assertThat(underTest.getAchievementInfo(PLAYER_ID, "aGameType"),
                is(equalTo(new AchievementInfo(1, 3))));
    }

    @Test(expected = NullPointerException.class)
    public void getPlayerAchievementsThrowsAnExceptionForANullPlayerId() {
        underTest.getPlayerAchievements(null);
    }

    @Test
    public void getPlayerAchievementsReturnsTheAchievementsForThePlayer() {
        when(playerAchievementsRepository.forPlayer(PLAYER_ID)).thenReturn(aPlayerAchievements());

        assertThat(underTest.getPlayerAchievements(PLAYER_ID), is(equalTo((Set<String>) newHashSet("two"))));
    }

    @Test(expected = NullPointerException.class)
    public void getAchievementDetailsThrowsAnExceptionForANullGameType() {
        underTest.getAchievementDetails(null);
    }

    @Test
    public void getAchievementDetailsReturnsDetailsForTheGameType() {
        when(achievementRepository.findByGameType("aGameType"))
                .thenReturn(newArrayList(anAchievement("one"), anAchievement("two"), anAchievement("three")));

        assertThat(underTest.getAchievementDetails("aGameType"),
                is(equalTo((Set<AchievementDetails>) newHashSet(anAchievementDetail("one"), anAchievementDetail("two"), anAchievementDetail("three")))));
    }

    private PlayerAchievements aPlayerAchievements() {
        return new PlayerAchievements(PLAYER_ID, newHashSet("two"), new HashMap<String, String>());
    }

    private AchievementDetails anAchievementDetail(final String name) {
        return new AchievementDetails(name, "aTitleFor" + name, 1, "aMessageFor" + name, "howToGetFor" + name);
    }

    private Achievement anAchievement(final String name) {
        return new Achievement(name, 1, "aTitleFor" + name, "aMessageFor" + name, "aShortDescFor" + name, "howToGetFor" + name,
                "postedTitleFor" + name, "postedLinkFor" + name, "postedActionTextFor" + name, "postedActionLinkFor" + name,
                "aGameType", new HashSet<String>(), "anAcc", "anAccParam", false);
    }

    private PlayerLevels aPlayerLevels() {
        final Map<String, PlayerLevel> playerLevels = new HashMap<String, PlayerLevel>();
        playerLevels.put("aGameType", aPlayerLevel());
        return new PlayerLevels(PLAYER_ID, playerLevels);
    }

    private PlayerLevel aPlayerLevel() {
        return new PlayerLevel(PLAYER_LEVEL, BigDecimal.valueOf(PLAYER_XP));
    }

    private LevelingSystem aLevelingSystem() {
        return new LevelingSystem("aGameType", Collections.<ExperienceFactor>emptySet(), levelDefinitions());
    }

    private List<LevelDefinition> levelDefinitions() {
        return newArrayList(
                new LevelDefinition(0, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN),
                new LevelDefinition(1, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(THIS_LEVEL_XP)),
                new LevelDefinition(PLAYER_LEVEL - 1, BigDecimal.valueOf(THIS_LEVEL_XP), BigDecimal.valueOf(NEXT_LEVEL_XP), BigDecimal.valueOf(1000)));
    }
}
