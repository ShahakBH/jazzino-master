package strata.server.worker.audit.playedtracking;

import com.yazino.platform.event.message.PlayerPlayedEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import strata.server.worker.audit.playedtracking.repository.PlayerPlayedEventRepository;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class LastPlayerPlayedEventsTest {

    private static final BigDecimal ACCOUNT_ID = BigDecimal.ONE;
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final DateTime TIME = new DateTime();

    private LastPlayerPlayedEvents underTest;
    private PlayerPlayedEventRepository playerPlayedEventRepository;
    private QueuePublishingService<PlayerPlayedEvent> publishingService;
    private PlayerPlayedEvent event;

    @Before
    public void setUp() throws Exception {
        playerPlayedEventRepository = mock(PlayerPlayedEventRepository.class);
        publishingService = mock(QueuePublishingService.class);
        underTest = new LastPlayerPlayedEvents(playerPlayedEventRepository, publishingService);
        event = new PlayerPlayedEvent(PLAYER_ID, TIME);
    }

    @Test
    public void shouldRetrieveLastTimestamp() {
        final long expected = TIME.getMillis();
        when(playerPlayedEventRepository.forAccount(ACCOUNT_ID)).thenReturn(new PlayerPlayedEvent(PLAYER_ID, TIME));
        final long actual = underTest.getLastEventTimestampForAccount(ACCOUNT_ID);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldRetrieveLastTimestampWhenNoEventIsStored() {
        assertEquals(-1, underTest.getLastEventTimestampForAccount(BigDecimal.valueOf(123)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireAccountIdToRetrieveEvent() {
        underTest.getLastEventTimestampForAccount(null);
    }

    @Test
    public void shouldStoreAndPublishEvent() {
        underTest.registerEvent(ACCOUNT_ID, PLAYER_ID, TIME);
        verify(playerPlayedEventRepository).store(ACCOUNT_ID, event);
        verify(publishingService).send(event);
    }

    @Test
    public void shouldIgnoreIfAccountDoesNotBelongToAPlayer() {
        underTest.registerEvent(ACCOUNT_ID, null, TIME);
        verifyZeroInteractions(publishingService, playerPlayedEventRepository);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireAccountIdToRegisterEvent() {
        underTest.registerEvent(null, PLAYER_ID, TIME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRequireTimestampToRegisterEvent() {
        underTest.registerEvent(ACCOUNT_ID, PLAYER_ID, null);
    }
}
