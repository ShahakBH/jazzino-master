package com.yazino.web.domain.social;

import com.yazino.platform.session.PlayerSessionStatus;
import com.yazino.web.data.SessionStatusRepository;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OnlineStatusRetrieverTest {

    private SessionStatusRepository repository;
    private OnlineStatusRetriever underTest;

    @Before
    public void setUp() throws Exception {
        repository = mock(SessionStatusRepository.class);
        underTest = new OnlineStatusRetriever(repository);
    }

    @Test
    public void shouldRetrieveOnlineStatus() {
        when(repository.getStatus(BigDecimal.ONE))
                .thenReturn(new PlayerSessionStatus(Collections.<String>emptySet(), BigDecimal.ONE));
        assertTrue((Boolean) underTest.retrieveInformation(BigDecimal.ONE, "gameType"));
    }
}
