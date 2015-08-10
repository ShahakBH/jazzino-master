package com.yazino.platform.persistence.statistic;

import com.yazino.platform.model.statistic.PlayerAchievements;
import com.yazino.platform.model.statistic.PlayerLevel;
import com.yazino.platform.model.statistic.PlayerLevels;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@TransactionConfiguration
public class JDBCPlayerStatsDAOIntegrationTest {

    public static final String PLAYER_NAME = "${name}";
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(-1);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(-1);

    @Autowired
    private JDBCPlayerStatsDAO underTest;

    @Autowired
    private JdbcTemplate jdbc;

    private PlayerAchievements playerAchievements;
    private PlayerLevels playerLevels;

    @Before
    public void setup() {
        jdbc.update("DELETE FROM ACCOUNT WHERE ACCOUNT_ID < 0");
        jdbc.update("DELETE FROM PLAYER WHERE PLAYER_ID < 0");
        createAccount(ACCOUNT_ID, "player test account 1");
        createPlayerProfile();
        createEmptyPlayer();
        playerAchievements = new PlayerAchievements(PLAYER_ID,
                new HashSet<String>(),
                new HashMap<String, String>());
        playerLevels = new PlayerLevels(PLAYER_ID, new HashMap<String, PlayerLevel>());
    }

    @Test
    public void shouldRetrieveEmptyPlayerLevels() {
        assertEquals(playerLevels, underTest.getLevels(PLAYER_ID));
    }

    @Test
    public void shouldReturnNullPlayerLevelForUnknownPlayer() {
        assertNull(underTest.getLevels(BigDecimal.valueOf(-2)));
    }

    @Test
    public void shouldRetrieveExistingPlayerLevels() {
        jdbc.update("UPDATE PLAYER SET LEVEL = ? WHERE PLAYER_ID=?",
                "gameType1\t23\t123\ngameType2\t53\t153",
                PLAYER_ID);
        playerLevels.updateLevel("gameType1", new PlayerLevel(23, BigDecimal.valueOf(123)));
        playerLevels.updateLevel("gameType2", new PlayerLevel(53, BigDecimal.valueOf(153)));
        assertEquals(playerLevels, underTest.getLevels(PLAYER_ID));
    }

    @Test
    public void shouldSavePlayerLevels() {
        playerLevels.updateLevel("gameType1", new PlayerLevel(23, BigDecimal.valueOf(123)));
        playerLevels.updateLevel("gameType2", new PlayerLevel(53, BigDecimal.valueOf(153)));
        underTest.saveLevels(playerLevels);
        final Map<String, Object> map = jdbc.queryForMap("SELECT LEVEL FROM PLAYER WHERE PLAYER_ID=?", PLAYER_ID);
        assertTrue(((String) map.get("LEVEL")).contains("gameType1\t23\t123"));
        assertTrue(((String) map.get("LEVEL")).contains("gameType2\t53\t153"));
    }

    @Test
    public void shouldReturnNullAchievementsForUnknownPlayer() {
        assertNull(underTest.getAchievements(BigDecimal.valueOf(-2)));
    }

    @Test
    public void shouldRetrieveEmptyAchievements() {
        assertEquals(playerAchievements, underTest.getAchievements(PLAYER_ID));
    }

    @Test
    public void shouldRetrieveExistingAchievements() {
        jdbc.update("UPDATE PLAYER SET ACHIEVEMENTS = ?, ACHIEVEMENT_PROGRESS = ? WHERE PLAYER_ID=?",
                "ach1\nach2\nach3",
                "ach4\t22\nach5\t15",
                PLAYER_ID);
        playerAchievements.awardAchievement("ach1");
        playerAchievements.awardAchievement("ach2");
        playerAchievements.awardAchievement("ach3");
        playerAchievements.updateProgressForAchievement("ach4", "22");
        playerAchievements.updateProgressForAchievement("ach5", "15");
        assertEquals(playerAchievements, underTest.getAchievements(PLAYER_ID));
    }

    @Test
    public void shouldSaveAchievements() {
        playerAchievements.awardAchievement("ach1");
        playerAchievements.awardAchievement("ach2");
        playerAchievements.awardAchievement("ach3");
        playerAchievements.updateProgressForAchievement("ach4", "22");
        playerAchievements.updateProgressForAchievement("ach5", "15");
        underTest.saveAchievements(playerAchievements);
        final Map<String, Object> map = jdbc.queryForMap("SELECT ACHIEVEMENTS, ACHIEVEMENT_PROGRESS" +
                " FROM PLAYER WHERE PLAYER_ID=?", PLAYER_ID);
        assertTrue(((String) map.get("ACHIEVEMENTS")).contains("ach1"));
        assertTrue(((String) map.get("ACHIEVEMENTS")).contains("ach2"));
        assertTrue(((String) map.get("ACHIEVEMENTS")).contains("ach3"));
        assertTrue(((String) map.get("ACHIEVEMENT_PROGRESS")).contains("ach4\t22"));
        assertTrue(((String) map.get("ACHIEVEMENT_PROGRESS")).contains("ach5\t15"));
    }

    private void createAccount(final BigDecimal id,
                               final String name) {
        jdbc.update("INSERT INTO ACCOUNT (ACCOUNT_ID,NAME) values (?,?)", id, name);
    }

    private void createEmptyPlayer() {
        jdbc.update("INSERT INTO PLAYER " +
                "(PLAYER_ID, NAME, ACCOUNT_ID, RELATIONSHIPS, PICTURE_LOCATION, " +
                "PREFERRED_CURRENCY, PREFERRED_PAYMENT_METHOD, TSCREATED) " +
                " values (?,?,?,?,?,?,?,now())",
                PLAYER_ID, PLAYER_NAME, ACCOUNT_ID, null, null, "gbp", null);
    }

    private void createPlayerProfile() {
        jdbc.update("INSERT INTO LOBBY_USER(PLAYER_ID,PROVIDER_NAME) VALUES(?,?)", PLAYER_ID, "YAZINO");
    }

}
