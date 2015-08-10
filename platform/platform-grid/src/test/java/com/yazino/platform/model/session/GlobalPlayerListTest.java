package com.yazino.platform.model.session;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.session.Location;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.table.TableType.PUBLIC;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class GlobalPlayerListTest {

    GlobalPlayerList underTest;

    @Mock
    private PlayerSessionRepository sessionRepository;
    private static final String GAME_1 = "a";
    private static final String GAME_2 = "b";

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        underTest = new GlobalPlayerList();
    }

    @Test
    public void whenPlayerChangesLocationAndHasLocationTheyWillBeAddedToTheGlobalListIfThereIsSpace() {
        final PlayerSession session = createPlayerSession(1, GAME_1);
        boolean result = underTest.playerLocationChanged(BigDecimal.valueOf(-1), asList(session), sessionRepository);
        assertEquals(true, result);
    }

    @Test
    public void whenPlayerGoesOfflineTheyAreRemovedFromTheList() {
        final int playerId = 1;
        setGlobalList(GAME_1, playerId);
        boolean result = underTest.playerGoesOffline(BigDecimal.valueOf(playerId));
        assertTrue(result);
    }

    @Test
    public void whenPlayerGoesOfflineAndTheyWereNotInTheListNoDocumentSent() throws Exception {
        final int playerId = 1;
        setGlobalList(GAME_1, playerId);
        boolean result = underTest.playerGoesOffline(BigDecimal.valueOf(playerId + 1));
        assertFalse(result);
    }

    @Test
    public void whenPlayerChangesLocationAndHasNoLocationTheyWillBeRemovedFromTheGlobalList() {
        setGlobalList(GAME_1, 1);
        boolean result = underTest.playerLocationChanged(BigDecimal.valueOf(1), asList(createPlayerSession(1)), sessionRepository);
        assertTrue(result);
    }

    @Test
    public void whenPlayerChangesLocationInDifferentGameAndIsAlreadyOnListTheyCanBeOn2Lists() throws Exception {
        final int playerId = 1;
        PlayerSession session = createPlayerSession(playerId, GAME_1);
        underTest.playerLocationChanged(BigDecimal.valueOf(playerId), asList(session), sessionRepository);
        assertEquals(1, underTest.currentLocations().size());
        session = createPlayerSession(playerId, GAME_1, GAME_2);
        underTest.playerLocationChanged(BigDecimal.valueOf(playerId), asList(session), sessionRepository);
        assertEquals(2, underTest.currentLocations().size());
        session = createPlayerSession(playerId, GAME_1);
        underTest.playerLocationChanged(BigDecimal.valueOf(playerId), asList(session), sessionRepository);
        assertEquals(1, underTest.currentLocations().size());
    }

    @Test
    public void whenGlobalListIsFullNoMorePlayerIdsWillBeAdded() {
        final int globalListSize = 6;
        underTest.setGlobalListSize(globalListSize);
        setGlobalList(GAME_1, 1, globalListSize);
        setGlobalList(GAME_2, 1, 3);
        final PlayerSession session = createPlayerSession(globalListSize + 1, GAME_1);
        boolean result = underTest.playerLocationChanged(BigDecimal.valueOf(globalListSize + 1), asList(session), sessionRepository);
        assertFalse(result);
    }

    @Test
    public void whenGlobalListIsFullPlayerIdsCanBeAddedToOtherGameTypesLists() {
        final int globalListSize = 6;
        underTest.setGlobalListSize(globalListSize);
        setGlobalList(GAME_1, 1, globalListSize);
        setGlobalList(GAME_2, 1, 3);
        final PlayerSession session = createPlayerSession(globalListSize + 1, GAME_2);
        boolean result = underTest.playerLocationChanged(BigDecimal.valueOf(globalListSize + 1), asList(session), sessionRepository);
        assertTrue(result);
    }

    private GlobalPlayers setGlobalList(final String gameType, int from, int to) {
        int[] items = new int[(to - from) + 1];
        int index = 0;
        int value = from;
        while (value <= to) {
            items[index++] = value++;
        }
        return setGlobalList(gameType, items);
    }

    @SuppressWarnings({"unchecked"})
    private GlobalPlayers setGlobalList(final String gameType, int... items) {
        final GlobalPlayers globalList = new GlobalPlayers(gameType, underTest.getGlobalListSize(), 60000);
        for (int i : items) {
            final PlayerSession ps = new PlayerSession(BigDecimal.valueOf(0 - i), BigDecimal.valueOf(i), ".",
                    "picture" + i, "player" + i, YAZINO, Platform.WEB, "127.0.0.1", BigDecimal.TEN, "playerEmail" + i);
            ps.addLocation(new Location("id", "name", gameType, null, PUBLIC));
            globalList.addPlayer(asList(ps), sessionRepository);
        }
        Map<String, GlobalPlayers> newGlobalList = (Map<String, GlobalPlayers>) ReflectionTestUtils.getField(underTest, "globalList");
        if (newGlobalList == null) {
            newGlobalList = new ConcurrentHashMap<>();
        }
        newGlobalList.put(gameType, globalList);
        ReflectionTestUtils.setField(underTest, "globalList", newGlobalList);
        return globalList;
    }

    public static PlayerSession createPlayerSession(int playerIdAsInt, String... gameTypes) {
        final BigDecimal playerId = BigDecimal.valueOf(playerIdAsInt);
        PlayerSession playerSession = new PlayerSession(BigDecimal.ZERO.subtract(playerId), playerId, "sessionKey",
                "pictureUrl", "nickname", Partner.YAZINO, Platform.WEB, "127.0.0.1", null, "playerEmail");
        for (int i = 0; i < gameTypes.length; i++) {
            playerSession.addLocation(createLocation(i, gameTypes[i]));
        }
        return playerSession;
    }

    public static Location createLocation(int locationId, String gameType) {
        return new Location(String.valueOf(locationId), "locationName", gameType, null, PUBLIC);
    }
}
