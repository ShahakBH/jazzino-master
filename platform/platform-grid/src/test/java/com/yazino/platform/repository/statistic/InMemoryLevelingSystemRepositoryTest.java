package com.yazino.platform.repository.statistic;

import com.yazino.platform.model.statistic.ExperienceFactor;
import com.yazino.platform.model.statistic.LevelDefinition;
import com.yazino.platform.model.statistic.LevelingSystem;
import com.yazino.platform.persistence.statistic.LevelingSystemDAO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

public class InMemoryLevelingSystemRepositoryTest {

    private LevelingSystemDAO levelingSystemDAO;
    private LevelingSystem levelingSystem1 = new LevelingSystem("gameType1", Collections.<ExperienceFactor>emptyList(), Collections.<LevelDefinition>emptyList());
    private LevelingSystem levelingSystem2 = new LevelingSystem("gameType2", Collections.<ExperienceFactor>emptyList(), Collections.<LevelDefinition>emptyList());
    private LevelingSystem levelingSystem3 = new LevelingSystem("gameType3", Collections.<ExperienceFactor>emptyList(), Collections.<LevelDefinition>emptyList());
    private InMemoryLevelingSystemRepository underTest;

    @Before
    public void setUp() throws Exception {
        levelingSystemDAO = Mockito.mock(LevelingSystemDAO.class);
        underTest = new InMemoryLevelingSystemRepository(levelingSystemDAO);
    }

    @Test
    public void shouldPopulateFromDAO() {
        Mockito.when(levelingSystemDAO.findAll()).thenReturn(Arrays.asList(levelingSystem1, levelingSystem2));
        Assert.assertEquals(levelingSystem1, underTest.findByGameType("gameType1"));
        Assert.assertEquals(levelingSystem2, underTest.findByGameType("gameType2"));
        Assert.assertNull(underTest.findByGameType("gameType3"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRefreshDefinition() {
        Mockito.when(levelingSystemDAO.findAll()).thenReturn(Arrays.asList(levelingSystem1), Arrays.asList(levelingSystem3));
        Assert.assertNull(underTest.findByGameType("gameType3"));
        underTest.refreshDefinitions();
        Assert.assertEquals(levelingSystem3, underTest.findByGameType("gameType3"));
    }
}
