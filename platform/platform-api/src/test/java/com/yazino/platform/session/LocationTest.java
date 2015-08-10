package com.yazino.platform.session;

import com.yazino.platform.table.TableType;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class LocationTest {

    private static final String LOCATION_ID = "location id";
    private static final String LOCATION_NAME = "location name";
    private static final String GAME_TYPE = "game type";
    private static final BigDecimal OWNER_ID = BigDecimal.TEN;

    @Test
    public void isPrivateLocationShouldReturnTrue() {
        Location underTest = new Location(LOCATION_ID, LOCATION_NAME, GAME_TYPE, OWNER_ID, TableType.PRIVATE);
        assertThat(underTest.isPrivateLocation(), is(true));
    }

    @Test
    public void isPrivateLocationShouldReturnFalse() {
        Location underTest = new Location(LOCATION_ID, LOCATION_NAME, GAME_TYPE, null, TableType.PUBLIC);
        assertThat(underTest.isPrivateLocation(), is(false));
    }

    @Test
    public void isTournamentLocationShouldReturnTrue() {
        Location underTest = new Location(LOCATION_ID, LOCATION_NAME, GAME_TYPE, null, TableType.TOURNAMENT);
        assertThat(underTest.isTournamentLocation(), is(true));
    }

    @Test
    public void isTournamentLocationShouldReturnFalse() {
        Location underTest = new Location(LOCATION_ID, LOCATION_NAME, GAME_TYPE, null, TableType.PUBLIC);
        assertThat(underTest.isTournamentLocation(), is(false));
    }
}
