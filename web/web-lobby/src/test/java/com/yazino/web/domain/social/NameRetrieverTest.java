package com.yazino.web.domain.social;

import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.web.data.BasicProfileInformationRepository;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NameRetrieverTest {
    private BasicProfileInformationRepository repository;
    private NameRetriever underTest;

    @Before
    public void setUp() throws Exception {
        repository = mock(BasicProfileInformationRepository.class);
        underTest = new NameRetriever(repository);
    }

    @Test
    public void shouldRetrieveName() {
        final BasicProfileInformation profile = new BasicProfileInformation(BigDecimal.ONE, "name", "pic", BigDecimal.ZERO);
        when(repository.getBasicProfileInformation(BigDecimal.ONE)).thenReturn(profile);
        assertEquals("name", underTest.retrieveInformation(BigDecimal.ONE, "aGameType"));
    }

    @Test
    public void shouldReturnNullIfProfileNotFound() {
        assertNull(underTest.retrieveInformation(BigDecimal.ONE, "aGameType"));
    }
}
