package com.yazino.platform.processor.table;

import com.yazino.platform.model.statistic.PlayerGameStatistics;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openspaces.core.GigaSpace;
import com.yazino.game.api.statistic.GameStatistic;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked"})
public class SpaceGameStatisticsPublisherTest {

    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(1);
    private static final String GAME_TYPE = "getGameType";
    private static final String CLIENT_ID = "clientId";

    private GigaSpace gigaSpace;
    private SpaceGameStatisticsPublisher underTest;

    @Before
    public void setUp() {
        gigaSpace = mock(GigaSpace.class);
        underTest = new SpaceGameStatisticsPublisher(gigaSpace);
    }

    @Test
    public void shouldWritePlayerStatisticsToSpace() {
        GameStatistic statPlayer1 = new GameStatistic(BigDecimal.valueOf(1), "stat1");
        GameStatistic anotherStatPlayer1 = new GameStatistic(BigDecimal.valueOf(1), "another stat");
        GameStatistic statPlayer2 = new GameStatistic(BigDecimal.valueOf(2), "stat2");
        Collection<GameStatistic> statistics = newHashSet(statPlayer1, statPlayer2, anotherStatPlayer1);
        underTest.publish(TABLE_ID, GAME_TYPE, CLIENT_ID, statistics);
        Set expected = newHashSet(
                new PlayerGameStatistics(BigDecimal.valueOf(1), TABLE_ID, GAME_TYPE, CLIENT_ID, newHashSet(statPlayer1, anotherStatPlayer1)),
                new PlayerGameStatistics(BigDecimal.valueOf(2), TABLE_ID, GAME_TYPE, CLIENT_ID, newHashSet(statPlayer2)));
        ArgumentCaptor<PlayerGameStatistics[]> captor = ArgumentCaptor.forClass(PlayerGameStatistics[].class);

        verify(gigaSpace).writeMultiple(captor.capture());

        Set actual = newHashSet(captor.getValue());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void shouldIgnoreIfStatsIsEmpty() {
        underTest.publish(TABLE_ID, GAME_TYPE, CLIENT_ID, Collections.<GameStatistic>emptyList());
        verifyZeroInteractions(gigaSpace);
    }

    @Test
    public void shouldIgnoreIfStatsIsNull() {
        underTest.publish(TABLE_ID, GAME_TYPE, CLIENT_ID, null);
        verifyZeroInteractions(gigaSpace);
    }
}
