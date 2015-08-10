package com.yazino.platform.repository.statistic;

import com.yazino.platform.model.statistic.Achievement;
import com.yazino.platform.persistence.statistic.AchievementDAO;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked"})
public class InMemoryAchievementRepositoryTest {

    public static final String GAME_TYPE_1 = "gameType1";
    public static final String GAME_TYPE_2 = "gameType2";
    private AchievementDAO achievementDAO;
    private InMemoryAchievementRepository underTest;
    private Collection<Achievement> allAchievements = new HashSet<Achievement>();

    @Before
    public void setUp() {
        achievementDAO = mock(AchievementDAO.class);
        allAchievements.add(achievement("achievement1", GAME_TYPE_2));
        allAchievements.add(achievement("achievement2", GAME_TYPE_1));
        allAchievements.add(achievement("achievement3", GAME_TYPE_2));
        when(achievementDAO.findAll()).thenReturn(allAchievements);
        underTest = new InMemoryAchievementRepository(achievementDAO);
    }

    @Test
    public void shouldLoadAllWhenFindingAll() {
        Collection<Achievement> actual = underTest.findAll();
        assertEquals(allAchievements, actual);
    }

    @Test
    public void shouldLoadAllOnlyOnce() {
        underTest.findAll();
        Collection<Achievement> actual = underTest.findAll();
        assertEquals(allAchievements, actual);
        verify(achievementDAO, times(1)).findAll();
    }

    @Test
    public void shouldFindByGameType() {
        Collection<Achievement> achievementsGameType1 = underTest.findByGameType(GAME_TYPE_1);
        Collection<Achievement> achievementsGameType2 = underTest.findByGameType(GAME_TYPE_2);
        Collection<Achievement> achievementsGameType3 = underTest.findByGameType("unknownGameType");
        assertEquals(set(achievement("achievement2", GAME_TYPE_1)), achievementsGameType1);
        assertEquals(set(achievement("achievement1", GAME_TYPE_2), achievement("achievement3", GAME_TYPE_2)), achievementsGameType2);
        assertEquals(set(), achievementsGameType3);
    }

    @Test
    public void shouldRefreshDefinitions() {
        underTest.findByGameType(GAME_TYPE_1);
        allAchievements.clear();
        allAchievements.add(achievement("achievementNew1", GAME_TYPE_1));
        allAchievements.add(achievement("achievementNew2", GAME_TYPE_1));
        underTest.refreshDefinitions();
        assertEquals(2, underTest.findByGameType(GAME_TYPE_1).size());
        assertEquals(2, underTest.findAll().size());
    }

    private HashSet set(Achievement... achievement) {
        return new HashSet(Arrays.asList(achievement));
    }

    private Achievement achievement(String id, String gameType) {
        return new Achievement(id, 1, "title", "msg", "shortDesc", "howToGet", "Achieved", "blackjack", "Play blackjack", "blackjack", gameType, Collections.<String>emptySet(), "acc", "accPar", true);
    }
}
