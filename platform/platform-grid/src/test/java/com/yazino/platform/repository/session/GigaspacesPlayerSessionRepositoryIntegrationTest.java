package com.yazino.platform.repository.session;

import com.yazino.platform.Platform;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.session.GlobalPlayerListUpdateRequest;
import com.yazino.platform.model.session.PlayerSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.yazino.platform.Partner.YAZINO;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspacesPlayerSessionRepositoryIntegrationTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);

    @Autowired
    private GigaSpace gigaSpace;
    @Autowired
    private GigaspacesPlayerSessionRepository underTest;

    private BigDecimal playerId = new BigDecimal("124.3");
    private BigDecimal playerIdB = new BigDecimal("345");
    private BigDecimal playerIdC = new BigDecimal("678");
    private PlayerSession playerSession = new PlayerSession(BigDecimal.ZERO.subtract(playerId), playerId, "1234", "http", "nick", YAZINO,
            Platform.WEB, "127.0.0.1", null, "email");
    private PlayerSession playerSessionB = new PlayerSession(BigDecimal.ZERO.subtract(playerIdB), playerIdB, "345", "ftp", "nick1", YAZINO,
            Platform.WEB, "127.0.0.1", null, "email");
    private PlayerSession playerSessionC = new PlayerSession(BigDecimal.ZERO.subtract(playerIdC), playerIdC, "6789", "teleport", "nick2", YAZINO,
            Platform.WEB, "127.0.0.1", null, "email");

    @Before
    public void cleanup() {
        gigaSpace.clear(null);
    }

    @Test
    public void online_returns_true_when_player_session_by_id_in_space() {
        gigaSpace.write(playerSession);
        assertTrue(underTest.isOnline(playerId));
        assertFalse(underTest.isOnline(playerId.subtract(BigDecimal.ONE)));
    }

    @Test
    public void getPlayer_finds_players_if_they_are_online() {
        gigaSpace.write(playerSession);
        final Collection<PlayerSession> sessions = underTest.findAllByPlayer(playerId);
        assertEquals(1, sessions.size());
        assertEquals(playerId, sessions.iterator().next().getPlayerId());
    }

    @Test
    public void save_stores_session_to_space() {
        underTest.save(playerSession);
        assertNotNull(gigaSpace.read(new PlayerSession(playerId)));
    }

    @Test
    public void testFindPlayersOnlineQueriesSpaceCorrectly() {
        gigaSpace.write(playerSession);
        gigaSpace.write(playerSessionB);
        gigaSpace.write(playerSessionC);
        Set<BigDecimal> playerIds = new HashSet<>();
        playerIds.add(playerId);
        playerIds.add(playerIdC);
        assertEquals(playerIds, underTest.findOnlinePlayers(playerIds));
    }

    @Test
    public void testFindPlayersOnlineQueriesSpaceCorrectlyMoreThanMaxPlayers() {
        int maxPlayers = 2000;
        int playerIdsSizeToRead = 2 * maxPlayers + 512;

        Set<BigDecimal> playerIds = new HashSet<>();

        for (int i = 1; i < playerIdsSizeToRead + 1; i++) {
            BigDecimal playerId = new BigDecimal(i);
            playerIds.add(playerId);

            gigaSpace.write(new PlayerSession(BigDecimal.ZERO.subtract(playerId), playerId, i + "123", "http", "nick" + i, YAZINO,
                    Platform.WEB, "127.0.0.1", null, "email"));
        }

        assertEquals(playerIds, underTest.findOnlinePlayers(playerIds));
    }

    @Test
    public void testFindPlayersOnlineNoSessionsPresent() {
        gigaSpace.write(playerSession);
        gigaSpace.write(playerSessionB);
        Set<BigDecimal> playerIds = new HashSet<>();
        playerIds.add(playerIdC);
        assertTrue(underTest.findOnlinePlayers(playerIds).isEmpty());
    }

    /*
       The SQL IN clause can handle more, it was tested with up to 50000 but has a problem with repeatition.
      */
    @Test
    public void testFindPlayersOnlineCanHandle2000ItemsInORClause() {
        Set<BigDecimal> playerIdsToSearch = new HashSet<>(2000);
        for (int count = 0; count < 2000; count++) {
            BigDecimal playerIdLocal = BigDecimal.valueOf(count);
            playerIdsToSearch.add(playerIdLocal);
            final PlayerSession session = new PlayerSession(playerIdLocal);
            session.setSessionId(playerIdLocal);
            gigaSpace.write(session);
        }
        assertEquals(playerIdsToSearch, underTest.findOnlinePlayers(playerIdsToSearch));
    }

    @Test
    @Transactional
    public void removeByPlayerId_drops_session_from_space() {
        final PlayerSession session = new PlayerSession(playerId);
        session.setSessionId(playerId);
        gigaSpace.write(session);
        underTest.removeAllByPlayer(playerId);
        assertNull(gigaSpace.readById(PlayerSession.class, playerId));
    }

    @Test
    @Transactional
    public void removeByPlayerAndSessionKey_drops_session_from_space() {
        final PlayerSession session = new PlayerSession(playerId);
        session.setSessionId(playerId);
        session.setLocalSessionKey("aSessionKey");
        gigaSpace.write(session);
        underTest.removeByPlayerAndSessionKey(playerId, "aSessionKey");
        assertNull(gigaSpace.readById(PlayerSession.class, playerId));
    }

    @Test
    @Transactional
    public void countSessionsReturnsNumberOfPlayerSessions() {
        for (int i = 0; i < 12; ++i) {
            final PlayerSession session = session(i);
            if (i % 2 == 0) {
                session.setPlaying(true);
            }
            gigaSpace.write(session);
        }

        assertEquals(12, underTest.countPlayerSessions(false));
    }

    @Test
    @Transactional
    public void countSessionsForPlayingPlayersReturnsNumberOfPlayingPlayerSessions() {
        for (int i = 0; i < 12; ++i) {
            final PlayerSession session = session(i);
            if (i % 2 == 0) {
                session.setPlaying(true);
            }
            gigaSpace.write(session);
        }

        assertEquals(6, underTest.countPlayerSessions(true));
    }

    @Test
    @Transactional
    public void updatingTheGlobalPlayerListWritesARequestToTheSpace() {
        underTest.updateGlobalPlayerList(PLAYER_ID);

        final GlobalPlayerListUpdateRequest request = gigaSpace.read(new GlobalPlayerListUpdateRequest());
        assertThat(request, is(not(nullValue())));
        assertThat(request.getPlayerId(), is(equalTo(PLAYER_ID)));
    }

    @Test(expected = NullPointerException.class)
    @Transactional
    public void updatingTheGlobalPlayerListRejectsANullPlayerId() {
        underTest.updateGlobalPlayerList(null);
    }

    @Test
    @Transactional
    public void findAllReturnsAPageOfSessions() {
        underTest.setPageSize(5);
        for (int i = 0; i < 5; ++i) {
            gigaSpace.write(session(i));
        }

        final PagedData<PlayerSession> sessions = underTest.findAll(0);

        assertThat(sessions, hasItems(session(0), session(1), session(2), session(3), session(4)));
        assertThat(sessions.getSize(), is(equalTo(5)));
        assertThat(sessions.getTotalSize(), is(equalTo(5)));
        assertThat(sessions.getStartPosition(), is(equalTo(0)));
    }

    @Test
    @Transactional
    public void findAllReturnsTheGivenPageOfSessionsSortedByPlayerId() {
        underTest.setPageSize(5);
        for (int i = 0; i < 15; ++i) {
            gigaSpace.write(session(i));
        }

        final PagedData<PlayerSession> sessions = underTest.findAll(2);

        assertThat(sessions, hasItems(session(10), session(11), session(12), session(13), session(14)));
        assertThat(sessions.getSize(), is(equalTo(5)));
        assertThat(sessions.getTotalSize(), is(equalTo(15)));
        assertThat(sessions.getStartPosition(), is(equalTo(10)));
    }

    @Test
    @Transactional
    public void findAllReturnsAnEmptyDataSetForAnInvalidPage() {
        underTest.setPageSize(5);
        for (int i = 0; i < 5; ++i) {
            gigaSpace.write(session(i));
        }

        final PagedData<PlayerSession> sessions = underTest.findAll(5);

        assertThat(sessions.getData().size(), is(equalTo(0)));
        assertThat(sessions.getSize(), is(equalTo(0)));
        assertThat(sessions.getTotalSize(), is(equalTo(5)));
        assertThat(sessions.getStartPosition(), is(equalTo(25)));
    }

    private PlayerSession session(final int i) {
        final PlayerSession playerSession = new PlayerSession(BigDecimal.valueOf(i));
        playerSession.setSessionId(BigDecimal.valueOf(i));
        return playerSession;
    }
}
