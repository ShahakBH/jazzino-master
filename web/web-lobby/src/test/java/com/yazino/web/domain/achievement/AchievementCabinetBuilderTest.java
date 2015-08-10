package com.yazino.web.domain.achievement;

import com.yazino.platform.playerstatistic.service.AchievementDetails;
import com.yazino.web.domain.PlayerAchievement;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AchievementCabinetBuilderTest {

    public static final String GAME_TYPE = "gameType";
    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(1);
    private AchievementDetailsRepository achievementDetailsRepository;
    private PlayerAchievementRepository playerAchievementRepository;
    private AchievementCabinetBuilder achievementCabinetBuilder;

    @Before
    public void setUp() {
        achievementDetailsRepository = mock(AchievementDetailsRepository.class);
        playerAchievementRepository = mock(PlayerAchievementRepository.class);
        achievementCabinetBuilder = new AchievementCabinetBuilder(achievementDetailsRepository, playerAchievementRepository);
    }

    @Test
    public void shouldBuildCabinet() {
        HashSet<AchievementDetails> achievementDetails = new HashSet<AchievementDetails>(asList(achievementDetails("a1", 1), achievementDetails("a2", 2)));
        when(achievementDetailsRepository.getAchievementDetails(GAME_TYPE)).thenReturn(achievementDetails);
        when(playerAchievementRepository.getPlayerAchievements(PLAYER_ID)).thenReturn(new HashSet<String>(asList("a1")));
        List<SortedSet<PlayerAchievement>> playerAchievements = new ArrayList<SortedSet<PlayerAchievement>>();
        playerAchievements.add(new TreeSet<PlayerAchievement>(asList(playerAchievements("a1", true))));
        playerAchievements.add(new TreeSet<PlayerAchievement>(asList(playerAchievements("a2", false))));
        playerAchievements.add(new TreeSet<PlayerAchievement>());
        AchievementCabinet expected = new AchievementCabinet(new int[]{1, 0, 0}, new int[]{1, 1, 0}, playerAchievements);
        AchievementCabinet actual = achievementCabinetBuilder.buildAchievementCabinet(PLAYER_ID, "aPlayer", GAME_TYPE);
        assertEquals(expected, actual);
    }

    private AchievementDetails achievementDetails(String id, int level) {
        return new AchievementDetails(id, "title", level, "message", "how to get");
    }

    private PlayerAchievement playerAchievements(String id, boolean collected) {
        return new PlayerAchievement(id, "title", 0, "message", "how to get", collected);
    }
}
