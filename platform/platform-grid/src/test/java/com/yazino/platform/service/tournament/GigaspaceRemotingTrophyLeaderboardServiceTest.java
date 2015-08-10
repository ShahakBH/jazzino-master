package com.yazino.platform.service.tournament;

import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import com.yazino.platform.tournament.TrophyLeaderboardDefinition;
import com.yazino.platform.tournament.TrophyLeaderboardException;
import com.yazino.platform.tournament.TrophyLeaderboardPosition;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceRemotingTrophyLeaderboardServiceTest {

    @Mock
    private TrophyLeaderboardRepository leaderboardRepository;
    @Mock
    private TrophyLeaderboardRepository leaderboardGlobalRepository;
    @Mock
    private GigaSpace gigaSpace;
    @Mock
    private GigaSpace globalGigaSpace;
    @Mock
    private TrophyLeaderboard leaderboard;

    private GigaspaceRemotingTrophyLeaderboardService underTest;

    @Before
    public void setUp() {
        underTest = new GigaspaceRemotingTrophyLeaderboardService(leaderboardRepository, globalGigaSpace);
    }

    @Test
    public void createShouldThrowExceptionIfExistingActiveLeaderboard() throws TrophyLeaderboardException {
        final TrophyLeaderboardDefinition definition = new TrophyLeaderboardDefinition("aName", "poker", new Interval(100, 1000),
                new Duration(1000), 0L, Collections.<Integer, TrophyLeaderboardPosition>emptyMap());
        final TrophyLeaderboard expectedTemplate = new TrophyLeaderboard();
        expectedTemplate.setActive(true);
        expectedTemplate.setGameType(definition.getGameType());
        when(globalGigaSpace.readIfExists(expectedTemplate)).thenReturn(new TrophyLeaderboard());

        try {
            underTest.create(definition);
            fail("Should have thrown a TrophyLeaderboardException");
        } catch (Exception e) {
            assertEquals("Could not create Trophy Leaderboard, an existing active board exists for gametype poker", e.getMessage());
        }
    }
}
