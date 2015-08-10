package com.yazino.platform.model.session;

import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.platform.Platform;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.session.Location;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collection;

import static com.yazino.platform.table.TableType.PUBLIC;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GlobalPlayersTest {
    private static final long TIMEOUT = 60000l;
    private static final String GAME_TYPE = "gameType";

    private final SettableTimeSource settableTimeSource = new SettableTimeSource(0);
    private final Location validLocation = new Location("id", "name", GAME_TYPE, null, PUBLIC);
    private PlayerSessionRepository playerSessionRepository;

    private GlobalPlayers underTest;

    @Before
    public void setUp() throws Exception {
        playerSessionRepository = mock(PlayerSessionRepository.class);
        underTest = new GlobalPlayers(GAME_TYPE, 20, TIMEOUT);
        ReflectionTestUtils.setField(underTest, "timeSource", settableTimeSource);
    }

    @Test
    public void shouldAddPlayer() {
        final Collection<PlayerSession> sessions = asList(createSession(1, validLocation));
        assertTrue(underTest.addPlayer(sessions, playerSessionRepository));
        assertEquals(1, underTest.getCurrentList().size());
    }

    @Test
    public void shouldNotAddIfNotPlayingGameType() {
        final Collection<PlayerSession> sessions = asList(createSession(1, new Location("id", "name", "otherGameType", null, PUBLIC)));
        assertFalse(underTest.addPlayer(sessions, playerSessionRepository));
        assertEquals(0, underTest.getCurrentList().size());
    }

    @Test
    public void shouldNotAddIfPlayingOnPrivateLocation() {
        final Collection<PlayerSession> sessions = asList(createSession(1, new Location("id", "name", "otherGameType", null, PUBLIC)));
        assertFalse(underTest.addPlayer(sessions, playerSessionRepository));
        assertEquals(0, underTest.getCurrentList().size());
    }

    @Test
    public void shouldNotAddIfLimitIsReached() {
        underTest = new GlobalPlayers(GAME_TYPE, 1, TIMEOUT);
        assertTrue(underTest.addPlayer(asList(createSession(1, validLocation)), playerSessionRepository));
        assertFalse(underTest.addPlayer(asList(createSession(2, validLocation)), playerSessionRepository));
        assertFalse(underTest.addPlayer(asList(createSession(3, validLocation)), playerSessionRepository));
    }

    @Test
    public void shouldIgnoreScavengeBeforeTimeout() {
        settableTimeSource.addMillis(TIMEOUT - 1);
        underTest.addPlayer(asList(createSession(1, validLocation)), playerSessionRepository);
        verifyNoMoreInteractions(playerSessionRepository);
    }

    @Test
    public void shouldKeepPlayerAfterTimeoutIfStillValid() {
        underTest.addPlayer(asList(createSession(1, validLocation)), playerSessionRepository);
        settableTimeSource.addMillis(TIMEOUT + 1);
        when(playerSessionRepository.findAllByPlayer(BigDecimal.valueOf(1))).thenReturn(asList(createSession(1, validLocation)));
        underTest.addPlayer(asList(createSession(2, validLocation)), playerSessionRepository);
        assertEquals(2, underTest.getCurrentList().size());
        verify(playerSessionRepository, times(1)).findAllByPlayer(any(BigDecimal.class));
    }

    @Test
    public void shouldRemovePlayerAfterTimeoutIfInvalid() {
        underTest.addPlayer(asList(createSession(1, validLocation)), playerSessionRepository);
        settableTimeSource.addMillis(TIMEOUT + 1);
        when(playerSessionRepository.findAllByPlayer(BigDecimal.valueOf(1))).thenReturn(asList(createSession(1)));
        underTest.addPlayer(asList(createSession(2, validLocation)), playerSessionRepository);
        assertEquals(1, underTest.getCurrentList().size());
        verify(playerSessionRepository, times(1)).findAllByPlayer(any(BigDecimal.class));
    }

    @Test
    public void shouldRemovePlayer() {
        assertFalse(underTest.remove(BigDecimal.valueOf(1)));
        underTest.addPlayer(asList(createSession(1, validLocation)), playerSessionRepository);
        assertEquals(1, underTest.getCurrentList().size());
        assertTrue(underTest.remove(BigDecimal.valueOf(1)));
        assertEquals(0, underTest.getCurrentList().size());
    }

    private PlayerSession createSession(final int playerIdAsInt, Location... locations) {
        final BigDecimal playerId = BigDecimal.valueOf(playerIdAsInt);
        final PlayerSession session = new PlayerSession(BigDecimal.ZERO.subtract(playerId), playerId, null,
                "pic" + playerIdAsInt, "nick" + playerIdAsInt, null, Platform.WEB, "127.0.0.1", BigDecimal.TEN, null);
        for (Location location : locations) {
            session.addLocation(location);
        }
        return session;
    }
}
