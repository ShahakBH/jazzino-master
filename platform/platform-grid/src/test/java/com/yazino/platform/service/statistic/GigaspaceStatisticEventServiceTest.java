package com.yazino.platform.service.statistic;

import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.model.statistic.PlayerStatistics;
import com.yazino.platform.playerstatistic.StatisticEvent;
import net.jini.core.lease.Lease;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import java.math.BigDecimal;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceStatisticEventServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(101);

    @Mock
    private GigaSpace gigaSpace;

    private GigaspaceStatisticEventService underTest;

    @Before
    public void setUp() {
        underTest = new GigaspaceStatisticEventService(gigaSpace);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullGigaSpace() {
        new GigaspaceStatisticEventService(null);
    }

    @Test(expected = NullPointerException.class)
    public void aPublishToANullPlayerThrowsAnException() {
        underTest.publishStatistics(null, "aGameType", newHashSet(aStatisticEvent()));
    }

    @Test
    public void aPublishPlacesAPlayerStatisticsInTheSpace() {
        underTest.publishStatistics(PLAYER_ID, "aGameType", newHashSet(aStatisticEvent()));

        verify(gigaSpace).write(new PlayerStatistics(PLAYER_ID, "aGameType", newHashSet(aStatisticEvent())),
                Lease.FOREVER, 5000, WriteModifiers.UPDATE_OR_WRITE);
    }

    private StatisticEvent aStatisticEvent() {
        return new StatisticEvent("anEvent");
    }

}
