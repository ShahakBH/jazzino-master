package strata.server.worker.audit.playedtracking.repository;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.yazino.platform.event.message.PlayerPlayedEvent;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class EhCachePlayerPlayedEventRepositoryTest {

    public static final DateTime TIME = new DateTime();
    private EhCachePlayerPlayedEventRepository underTest;
    private Ehcache cache;
    private static final BigDecimal ACCOUNT_ID = BigDecimal.ONE;
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;

    @Before
    public void setUp() throws Exception {
        cache = mock(Ehcache.class);
        underTest = new EhCachePlayerPlayedEventRepository(cache);
    }

    @Test
    public void shouldRetrieveEventFromCache() {
        final PlayerPlayedEvent expected = new PlayerPlayedEvent(PLAYER_ID, TIME);
        when(cache.get(ACCOUNT_ID)).thenReturn(new Element(ACCOUNT_ID, expected, 1l));
        final PlayerPlayedEvent actual = underTest.forAccount(ACCOUNT_ID);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldReturnNullIfEventNotInCache() {
        assertNull(underTest.forAccount(BigDecimal.valueOf(123)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNoAccountIdIsProvided() {
        underTest.forAccount(null);
    }

    @Test
    public void shouldStoreInCache() {
        final PlayerPlayedEvent event = new PlayerPlayedEvent(PLAYER_ID, TIME);
        underTest.store(ACCOUNT_ID, event);
        verify(cache).put(new Element(ACCOUNT_ID, event));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNoAccountIdIsProvidedOnStore() {
        underTest.store(null, new PlayerPlayedEvent(PLAYER_ID, TIME));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfNoEventIsProvidedOnStore() {
        underTest.store(ACCOUNT_ID, null);
    }

}
