package com.yazino.web.domain.social;

import com.yazino.web.data.LevelRepository;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class LevelRetrieverTest {

    private LevelRetriever underTest;
    private LevelRepository repository;

    @Before
    public void setUp() throws Exception {
        repository = mock(LevelRepository.class);
        underTest = new LevelRetriever(repository);
    }

    @Test
    public void shouldReturnPlayerLevel() {
        when(repository.getLevel(BigDecimal.ONE, "aGameType")).thenReturn(3);
        assertEquals(3, underTest.retrieveInformation(BigDecimal.ONE, "aGameType"));
    }

    @Test
    public void shouldReturnNullIfGameTypeNotPresent() {
        assertNull(underTest.retrieveInformation(BigDecimal.ONE, null));
        verifyZeroInteractions(repository);
    }
}
