package com.yazino.platform.service.statistic;

import com.yazino.platform.repository.statistic.AchievementRepository;
import com.yazino.platform.repository.statistic.LevelingSystemRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceRemotingPlayerStatsBackOfficeServiceTest {

    @Mock
    private AchievementRepository achievementRepository;
    @Mock
    private LevelingSystemRepository levelingSystemRepository;

    private GigaspaceRemotingPlayerStatsBackOfficeService underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new GigaspaceRemotingPlayerStatsBackOfficeService(achievementRepository, levelingSystemRepository);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullAchievementRepository() {
        new GigaspaceRemotingPlayerStatsBackOfficeService(null, levelingSystemRepository);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullLevelingSystemRepository() {
        new GigaspaceRemotingPlayerStatsBackOfficeService(achievementRepository, null);
    }

    @Test
    public void shouldRefreshAchievementDefinitions() {
        underTest.refreshAchievements();

        verify(achievementRepository).refreshDefinitions();
    }

    @Test
    public void shouldRefreshLevelDefinitions() {
        underTest.refreshLevelDefinitions();

        verify(levelingSystemRepository).refreshDefinitions();
    }
}
