package strata.server.worker.event.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.LeaderboardEvent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.event.persistence.PostgresLeaderboardDWDAO;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LeaderboardEventConsumerTest {
    @Mock
    private PostgresLeaderboardDWDAO externalDao;
    @Mock
    private YazinoConfiguration configuration;

    private LeaderboardEventConsumer underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new LeaderboardEventConsumer(configuration, externalDao);
    }

    @Test
    public void shouldSaveToExternalDAOWhenEnabled() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aLeaderboardEvent(1));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(aLeaderboardEvent(1)));
    }

    @Test
    public void shouldSaveMultipleEventsToTheExternalDAOWhenEnabled() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aLeaderboardEvent(1));
        underTest.handle(aLeaderboardEvent(2));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(aLeaderboardEvent(1), aLeaderboardEvent(2)));
    }

    @Test
    public void shouldFlushBatchOnCommit() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aLeaderboardEvent(1));
        underTest.handle(aLeaderboardEvent(2));
        underTest.consumerCommitting();
        underTest.handle(aLeaderboardEvent(3));
        underTest.handle(aLeaderboardEvent(4));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(aLeaderboardEvent(1), aLeaderboardEvent(2)));
        verify(externalDao).save(newHashSet(aLeaderboardEvent(3), aLeaderboardEvent(4)));
    }

    @Test
    public void shouldNotSaveToExternalDAOWhenDisabled() {
        underTest.handle(aLeaderboardEvent(1));
        underTest.consumerCommitting();

        verifyZeroInteractions(externalDao);
    }

    private LeaderboardEvent aLeaderboardEvent(final int leaderboardId) {
        return new LeaderboardEvent(BigDecimal.valueOf(leaderboardId), "GameType", new DateTime(10000000L), defaultPositions());
    }

    private Map<Integer, BigDecimal> defaultPositions() {
        final Map<Integer, BigDecimal> positions = new HashMap<>();
        positions.put(1, BigDecimal.valueOf(1000));
        positions.put(2, BigDecimal.valueOf(2000));
        positions.put(3, BigDecimal.valueOf(3000));
        positions.put(4, BigDecimal.valueOf(4000));
        return positions;
    }
}
