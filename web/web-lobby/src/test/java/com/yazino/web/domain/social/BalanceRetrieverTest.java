package com.yazino.web.domain.social;

import com.yazino.web.data.BalanceSnapshotRepository;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BalanceRetrieverTest {
    private BalanceSnapshotRepository repository;
    private BalanceRetriever underTest;

    @Before
    public void setUp() throws Exception {
        repository = mock(BalanceSnapshotRepository.class);
        underTest = new BalanceRetriever(repository);
    }

    @Test
    public void shouldRetrieveBalance() {
        when(repository.getBalanceSnapshot(BigDecimal.ONE)).thenReturn(BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, underTest.retrieveInformation(BigDecimal.ONE, "aGameType"));
    }

    @Test
    public void shouldReturnNullIfBalanceNotAvailable() {
        when(repository.getBalanceSnapshot(BigDecimal.ONE)).thenThrow(new RuntimeException("error"));
        assertNull(underTest.retrieveInformation(BigDecimal.ONE, "aGameType"));
    }
    
}
