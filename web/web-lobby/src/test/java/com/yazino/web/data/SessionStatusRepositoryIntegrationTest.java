package com.yazino.web.data;

import com.yazino.platform.session.PlayerSessionStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class SessionStatusRepositoryIntegrationTest {

    @Autowired
    private SessionStatusRepository underTest;

    @Autowired
    private SessionServiceStub sessionService;

    @Test
    @DirtiesContext
    public void shouldRetrieveOffline() throws IOException {
        assertNull(underTest.getStatus(BigDecimal.ONE));
    }

    @Test
    @DirtiesContext
    public void shouldRetrieveOnlineAsOfflineBeforeRefresh() {
        sessionService.addStatus(new PlayerSessionStatus(BigDecimal.ONE));
        assertNull(underTest.getStatus(BigDecimal.ONE));
    }

    @Test
    @DirtiesContext
    public void shouldRetrieveOnlineAfterRefresh() {
        final PlayerSessionStatus expected = new PlayerSessionStatus(BigDecimal.ONE);
        sessionService.addStatus(expected);
        underTest.refresh();
        final PlayerSessionStatus actual = underTest.getStatus(BigDecimal.ONE);
        assertNotNull(actual);
        assertEquals(expected, actual);
    }
    
    @Test
    @DirtiesContext
    public void shouldRetrieveOfflineAfterRefresh() {
        final PlayerSessionStatus expected = new PlayerSessionStatus(BigDecimal.ONE);
        sessionService.addStatus(expected);
        underTest.refresh();
        sessionService.removeStatus(expected);
        underTest.refresh();
        assertNull(underTest.getStatus(BigDecimal.ONE));
    }
}
