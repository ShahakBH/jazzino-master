package strata.server.worker.event.consumer;

import com.google.common.collect.Lists;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.PlayerEvent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.worker.event.persistence.PostgresPlayerDWDAO;

import java.util.Collections;

import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static org.mockito.Mockito.*;

public class PlayerEventConsumerTest {

    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private PostgresPlayerDWDAO pgDao;
    private PlayerEventConsumer underTest;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        underTest = new PlayerEventConsumer(yazinoConfiguration, pgDao);
    }

    @Test
    public void shouldSaveUsingPgDao() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);
        final PlayerEvent message = new PlayerEvent(ZERO, new DateTime(), TEN, Collections.<String>emptySet());
        underTest.handle(message);
        underTest.consumerCommitting();
        verify(pgDao).saveAll(Lists.newArrayList(message));
    }

    @Test
    public void shouldDisableSaveUsingPgDao() {
        final PlayerEvent message = new PlayerEvent(ZERO, new DateTime(), TEN, Collections.<String>emptySet());
        underTest.handle(message);
        underTest.consumerCommitting();

        verifyZeroInteractions(pgDao);
    }
}
