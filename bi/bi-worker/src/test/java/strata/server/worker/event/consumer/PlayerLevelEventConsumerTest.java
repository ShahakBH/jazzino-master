package strata.server.worker.event.consumer;

import com.google.common.collect.Lists;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.PlayerLevelEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.worker.event.persistence.PostgresPlayerLevelDWDAO;

import java.math.BigDecimal;
import java.util.ArrayList;

import static java.math.BigDecimal.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.*;

public class PlayerLevelEventConsumerTest {

    PlayerLevelEventConsumer underTest;
    @Mock
    YazinoConfiguration configuration;
    @Mock
    PostgresPlayerLevelDWDAO pgDao;

    @Captor
    private ArgumentCaptor<ArrayList<PlayerLevelEvent>> captor;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        underTest = new PlayerLevelEventConsumer(configuration, pgDao);
    }

    @Test
    public void shouldSaveUsingPostgresDao() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);
        final PlayerLevelEvent message = aPlayerLevelEvent(BigDecimal.ONE);
        underTest.handle(message);
        underTest.consumerCommitting();
        verify(pgDao).saveAll(Lists.newArrayList(message));
    }

    @Test
    public void shouldSaveMultipleMessagesOnCommitUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aPlayerLevelEvent(valueOf(1)));
        underTest.handle(aPlayerLevelEvent(valueOf(2)));
        underTest.handle(aPlayerLevelEvent(valueOf(3)));
        underTest.consumerCommitting();


        verify(pgDao).saveAll(captor.capture());
        assertThat(captor.getValue(), containsInAnyOrder(aPlayerLevelEvent(valueOf(1)), aPlayerLevelEvent(valueOf(2)), aPlayerLevelEvent(valueOf(3))));
    }

    @Test
    public void shouldFlushMessageOnCommitUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aPlayerLevelEvent(valueOf(1)));
        underTest.handle(aPlayerLevelEvent(valueOf(2)));
        underTest.handle(aPlayerLevelEvent(valueOf(3)));
        underTest.consumerCommitting();

        verify(pgDao).saveAll(captor.capture());
        assertThat(captor.getValue(), containsInAnyOrder(aPlayerLevelEvent(valueOf(1)), aPlayerLevelEvent(valueOf(2)), aPlayerLevelEvent(valueOf(3))));

        underTest.handle(aPlayerLevelEvent(valueOf(4)));
        underTest.handle(aPlayerLevelEvent(valueOf(5)));
        underTest.consumerCommitting();

        verify(pgDao, times(2)).saveAll(captor.capture());
        assertThat(captor.getValue(), containsInAnyOrder(aPlayerLevelEvent(valueOf(4)), aPlayerLevelEvent(valueOf(5))));
    }

    @Test
    public void shouldDisableSaveUsingPostgresqlDAO() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(false);
        final PlayerLevelEvent message = aPlayerLevelEvent(BigDecimal.ONE);

        underTest.handle(message);
        underTest.consumerCommitting();

        verifyZeroInteractions(pgDao);
    }


    private PlayerLevelEvent aPlayerLevelEvent(final BigDecimal id) {
        return new PlayerLevelEvent(id.toString(), "gameType", "level");
    }
}
