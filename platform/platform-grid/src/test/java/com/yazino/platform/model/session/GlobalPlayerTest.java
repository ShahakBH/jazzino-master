package com.yazino.platform.model.session;

import com.yazino.platform.Platform;
import com.yazino.platform.session.InvalidPlayerSessionException;
import com.yazino.platform.session.Location;
import com.yazino.platform.table.TableType;
import org.junit.Test;

import java.math.BigDecimal;

import static com.yazino.platform.Partner.YAZINO;
import static com.yazino.platform.table.TableType.PRIVATE;
import static com.yazino.platform.table.TableType.PUBLIC;
import static org.junit.Assert.assertEquals;

public class GlobalPlayerTest {

    @Test
    public void shouldCreateWithSpecificGameType() throws InvalidPlayerSessionException {
        final Location l1 = new Location("location1", "location1", "gameType1", null, PUBLIC);
        final Location l2 = new Location("location2", "location2", "gameType2", null, PUBLIC);
        final Location l3 = new Location("location3", "location3", "gameType3", null, PUBLIC);
        final GlobalPlayer underTest = new GlobalPlayer("gameType2", createSession("gameType2", l1, l2, l3).getPlayerId(), createSession("gameType2", l1, l2, l3).getNickname(), createSession("gameType2", l1, l2, l3).getPictureUrl(), createSession("gameType2", l1, l2, l3).getBalanceSnapshot(), createSession("gameType2", l1, l2, l3).getLocations());
        assertEquals(l2, underTest.getLocation());
    }

    @Test
    public void shouldCreateIfLevelNotDefined() throws InvalidPlayerSessionException {
        final PlayerSession session = createSession("otherGameType", new Location("id", "loc", "gameType2", null, PUBLIC));
        new GlobalPlayer("gameType2", session.getPlayerId(), session.getNickname(), session.getPictureUrl(), session.getBalanceSnapshot(), session.getLocations());
    }

    @Test(expected = InvalidPlayerSessionException.class)
    public void shouldNotCreateIfThereIsNotAtLeastOneLocationForGameType() throws InvalidPlayerSessionException {
        final PlayerSession session = createSession("gameType2", new Location("id", "loc", "otherGameType", null, PUBLIC));
        new GlobalPlayer("gameType2", session.getPlayerId(), session.getNickname(), session.getPictureUrl(), session.getBalanceSnapshot(), session.getLocations());
    }

    @Test(expected = InvalidPlayerSessionException.class)
    public void shouldNotCreateIfThereIsNotAtLeastOnePublicLocation() throws InvalidPlayerSessionException {
        final PlayerSession session = createSession("gameType2", new Location("id", "loc", "gameType2", BigDecimal.valueOf(1231), PRIVATE));
        new GlobalPlayer("gameType2", session.getPlayerId(), session.getNickname(), session.getPictureUrl(), session.getBalanceSnapshot(), session.getLocations());
    }

    private PlayerSession createSession(final String gameTypeForLevel, Location... locations) {
        final PlayerSession result = new PlayerSession(BigDecimal.ZERO, BigDecimal.ONE, "", "pic", "nick", YAZINO,
                Platform.WEB, "127.0.0.1", BigDecimal.TEN, "email");
        for (Location location : locations) {
            result.addLocation(location);
        }
        return result;
    }
}
