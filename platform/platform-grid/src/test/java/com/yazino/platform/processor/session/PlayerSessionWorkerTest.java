package com.yazino.platform.processor.session;

import com.yazino.platform.Platform;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.LocationChange;
import com.yazino.platform.session.LocationChangeType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.table.TableType.PUBLIC;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayerSessionWorkerTest {
    private static final String GAME_TYPE = "BLACKJACK";
    private static final BigDecimal PLAYER_ID = new BigDecimal("11.11");
    private static final String PLAYER_EMAIL = "email";
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);

    private PlayerSessionRepository sessionRepository;

    private PlayerSessionWorker underTest;

    @Before
    public void wireMocks() {
        sessionRepository = mock(PlayerSessionRepository.class);

        underTest = new PlayerSessionWorker(sessionRepository);
    }

    @Test
    public void authenticateAndExtendLobbySession_returns_session_if_OK() {
        final String localSessionKey = "SKSKSKS";

        when(sessionRepository.findByPlayerAndSessionKey(PLAYER_ID, localSessionKey)).thenReturn(
                new PlayerSession(SESSION_ID, PLAYER_ID, localSessionKey, "http://url", "pera", YAZINO, Platform.WEB, "127.0.0.1", null, PLAYER_EMAIL));

        assertNotNull("correct key", underTest.authenticate(PLAYER_ID, localSessionKey));
        assertNull("incorrect key", underTest.authenticate(PLAYER_ID, localSessionKey + "X"));
        assertNull("no session", underTest.authenticate(BigDecimal.TEN, localSessionKey));
    }

    @Test
    public void touch_modifies_timestamp() {
        PlayerSession session = new PlayerSession(BigDecimal.valueOf(133));
        session.setTimestamp(null);
        underTest.touch(session);
        assertNotNull(session.getTimestamp());
    }

    @Test
    public void test_add_new_location_for_all_sessions() {
        PlayerSession ps = aSession();
        underTest.processLocationChange(ps, new LocationChange(PLAYER_ID, null, LocationChangeType.ADD, aPublicLocation()));
        assertTrue(ps.getLocations().contains(new Location("12345", "NAME", GAME_TYPE, null, PUBLIC)));
    }

    @Test
    public void test_add_new_location_for_matched_specific_session() {
        PlayerSession ps = aSession();
        underTest.processLocationChange(ps, new LocationChange(PLAYER_ID, SESSION_ID, LocationChangeType.ADD, aPublicLocation()));
        assertTrue(ps.getLocations().contains(new Location("12345", "NAME", GAME_TYPE, null, PUBLIC)));
    }

    @Test
    public void test_ignores_new_location_for_unmatched_specific_session() {
        PlayerSession ps = aSession();
        underTest.processLocationChange(ps, new LocationChange(PLAYER_ID, BigDecimal.ONE, LocationChangeType.ADD, aPublicLocation()));
        assertFalse(ps.getLocations().contains(new Location("12345", "NAME", GAME_TYPE, null, PUBLIC)));
    }

    @Test
    public void test_remove_location() {
        PlayerSession ps = aSession();
        ps.addLocation(new Location("12345", "NAME", GAME_TYPE, null, PUBLIC));
        ps.addLocation(new Location("6677", "NAME2", GAME_TYPE, null, PUBLIC));

        underTest.processLocationChange(ps, new LocationChange(PLAYER_ID, SESSION_ID, LocationChangeType.REMOVE, aPublicLocation()));
        Assert.assertFalse(ps.getLocations().contains(new Location("12345", "NAME", GAME_TYPE, null, PUBLIC)));
        assertTrue(ps.getLocations().contains(new Location("6677", "NAME2", GAME_TYPE, null, PUBLIC)));
    }

    private Location aPublicLocation() {
        return new Location("12345", "NAME", GAME_TYPE, null, PUBLIC);
    }

    private PlayerSession aSession() {
        return new PlayerSession(SESSION_ID, PLAYER_ID, "ls", "url", "nickname", YAZINO, Platform.WEB, "127.0.0.1", null, PLAYER_EMAIL);
    }
}
