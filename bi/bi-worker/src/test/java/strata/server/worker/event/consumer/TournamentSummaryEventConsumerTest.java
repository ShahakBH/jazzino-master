package strata.server.worker.event.consumer;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.TournamentPlayerSummary;
import com.yazino.platform.event.message.TournamentSummaryEvent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.worker.event.persistence.PostgresTournamentDWDAO;

import java.math.BigDecimal;
import java.util.Collections;

import static com.google.common.collect.Sets.newHashSet;
import static java.math.BigDecimal.valueOf;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TournamentSummaryEventConsumerTest {
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.ONE;
    private static final BigDecimal TEMPLATE_ID = BigDecimal.TEN;
    private static final String GAME_TYPE = "aGameType";
    private static final String TOURNAMENT_NAME = "TournamentName";

    @Mock
    private PostgresTournamentDWDAO externalDao;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private TournamentSummaryEventConsumer underTest;

    @Before
    public void setUp() {
        underTest = new TournamentSummaryEventConsumer(yazinoConfiguration, externalDao);
    }

    @Test
    public void shouldSaveUsingExternalDaoWhenRedshiftPropertyIsTrue() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aSummaryEvent(TOURNAMENT_ID));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(aSummaryEvent(TOURNAMENT_ID)));
    }

    @Test
    public void shouldSaveMultipleMessagesUsingExternalDaoWhenRedshiftPropertyIsTrue() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aSummaryEvent(valueOf(1)));
        underTest.handle(aSummaryEvent(valueOf(2)));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(aSummaryEvent(valueOf(1)), aSummaryEvent(valueOf(2))));
    }

    @Test
    public void shouldFlushExternalBatchOnCommit() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(true);

        underTest.handle(aSummaryEvent(valueOf(1)));
        underTest.handle(aSummaryEvent(valueOf(2)));
        underTest.consumerCommitting();
        underTest.handle(aSummaryEvent(valueOf(3)));
        underTest.handle(aSummaryEvent(valueOf(4)));
        underTest.consumerCommitting();

        verify(externalDao).save(newHashSet(aSummaryEvent(valueOf(1)), aSummaryEvent(valueOf(2))));
        verify(externalDao).save(newHashSet(aSummaryEvent(valueOf(3)), aSummaryEvent(valueOf(4))));
    }

    @Test
    public void shouldNotSaveUsingExternalDaoWhenRedshiftPropertyIsFalse() {
        when(yazinoConfiguration.getBoolean("data-warehouse.write.enabled")).thenReturn(false);

        underTest.handle(aSummaryEvent(TOURNAMENT_ID));
        underTest.consumerCommitting();

        verifyZeroInteractions(externalDao);
    }

    private TournamentSummaryEvent aSummaryEvent(final BigDecimal tournamentId) {
        return new TournamentSummaryEvent(tournamentId, TOURNAMENT_NAME, TEMPLATE_ID, GAME_TYPE,
                new DateTime(10000), new DateTime(11000), Collections.<TournamentPlayerSummary>emptySet());
    }
}
