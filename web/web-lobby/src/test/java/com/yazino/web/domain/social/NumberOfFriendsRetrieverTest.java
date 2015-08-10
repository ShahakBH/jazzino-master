package com.yazino.web.domain.social;

import com.yazino.web.util.PlayerFriendsCache;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyChar;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NumberOfFriendsRetrieverTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;
    private PlayerFriendsCache cache;
    private NumberOfFriendsRetriever underTest;

    @Before
    public void setUp() throws Exception {
        cache = mock(PlayerFriendsCache.class);
        underTest = new NumberOfFriendsRetriever(cache);
    }

    @Test
    public void shouldRetrieveNumberOfFriendsFromCache(){
        when(cache.getFriendIds(PLAYER_ID)).thenReturn(new HashSet<BigDecimal>(Arrays.asList(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)));
        final Object actual = underTest.retrieveInformation(PLAYER_ID, null);
        assertEquals(3, actual);
    }

    @Test
    public void shouldStillRetrieveNumberOfFriendsIfExceptionHappens(){
        when(cache.getFriendIds(PLAYER_ID)).thenThrow(new NullPointerException("player not found"));
        final Object actual = underTest.retrieveInformation(PLAYER_ID, null);
        assertEquals(0, actual);
    }
}
