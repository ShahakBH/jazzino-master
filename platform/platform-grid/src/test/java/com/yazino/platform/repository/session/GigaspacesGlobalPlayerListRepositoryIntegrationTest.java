package com.yazino.platform.repository.session;

import com.yazino.platform.Platform;
import com.yazino.platform.model.session.GlobalPlayerList;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.session.Location;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.table.TableType.PUBLIC;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspacesGlobalPlayerListRepositoryIntegrationTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.ONE;

    @Autowired
    private GlobalPlayerListRepository globalPlayerListRepository;

    private PlayerSessionRepository playerSessionRepository = mock(PlayerSessionRepository.class);

    @Test
    @Transactional
    public void shouldCreateOnLock() {
        final GlobalPlayerList result = globalPlayerListRepository.lock();
        assertNotNull(result);
    }

    @Test
    public void shouldCreateOnRead() {
        final GlobalPlayerList result = globalPlayerListRepository.read();
        assertNotNull(result);
    }

    @Test
    @Transactional
    public void shouldUpdate() {
        final GlobalPlayerList gpl = globalPlayerListRepository.lock();
        final PlayerSession pSession = new PlayerSession(BigDecimal.valueOf(3141592), PLAYER_ID, "lsk", "pic", "name", YAZINO, Platform.WEB, "127.0.0.1", BigDecimal.TEN, "email");
        pSession.addLocation(new Location("123", "location1", "gameType", null, PUBLIC));
        gpl.playerLocationChanged(PLAYER_ID, asList(pSession), playerSessionRepository);
        globalPlayerListRepository.save(gpl);
        final GlobalPlayerList updatedList = globalPlayerListRepository.read();
        assertEquals(1, updatedList.currentLocations().size());
    }
}
