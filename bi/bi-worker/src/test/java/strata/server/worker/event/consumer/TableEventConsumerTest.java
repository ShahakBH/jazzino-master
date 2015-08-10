package strata.server.worker.event.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.TableEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.event.persistence.PostgresTableDWDAO;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.math.BigDecimal.valueOf;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TableEventConsumerTest {
    private static final BigDecimal TABLE_ID = BigDecimal.ONE;
    private static final BigDecimal TEMPLATE_ID = BigDecimal.TEN;
    private static final String GAME_TYPE_ID = "gameTypeId";
    private static final String TEMPLATE_NAME = "templateName";

    @Mock
    private PostgresTableDWDAO externalDao;
    @Mock
    YazinoConfiguration configuration;

    private TableEventConsumer underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new TableEventConsumer(configuration, externalDao);
    }

    @Test
    public void shouldSaveUsingPostgresDaoWhenRedshiftIsEnabled() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aTableEvent(TABLE_ID));
        underTest.consumerCommitting();

        verify(externalDao).saveAll(newArrayList(aTableEvent(TABLE_ID)));
    }

    @Test
    public void shouldSaveMultipleEventsUsingPostgresDaoWhenRedshiftIsEnabled() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aTableEvent(TABLE_ID));
        underTest.handle(aTableEvent(valueOf(11)));
        underTest.consumerCommitting();

        verify(externalDao).saveAll(newArrayList(aTableEvent(TABLE_ID), aTableEvent(valueOf(11))));
    }

    @Test
    public void shouldFlushExternalBatchOnCommit() {
        when(configuration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aTableEvent(valueOf(1)));
        underTest.handle(aTableEvent(valueOf(2)));
        underTest.consumerCommitting();

        final Set<TableEvent> expected = new HashSet<>();
        expected.add(aTableEvent(valueOf(1)));
        expected.add(aTableEvent(valueOf(2)));
        verify(externalDao).saveAll(newArrayList(expected));

        underTest.handle(aTableEvent(valueOf(3)));
        underTest.handle(aTableEvent(valueOf(4)));
        underTest.consumerCommitting();

        expected.clear();
        expected.add(aTableEvent(valueOf(3)));
        expected.add(aTableEvent(valueOf(4)));
        verify(externalDao).saveAll(newArrayList(expected));
    }

    @Test
    public void shouldNotSaveUsingPostgresDaoWhenRedshiftIsDisabled() {
        underTest.handle(aTableEvent(TABLE_ID));
        underTest.consumerCommitting();

        verifyZeroInteractions(externalDao);
    }

    private TableEvent aTableEvent(final BigDecimal tableId) {
        return new TableEvent(tableId, GAME_TYPE_ID, TEMPLATE_ID, TEMPLATE_NAME);
    }
}
