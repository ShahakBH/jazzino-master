package com.yazino.web.service;

import com.yazino.platform.table.CountdownService;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GameAvailabilityServiceTest {

    private static final String GAME_TYPE = "some-game-type";
    private static final String ALL_GAME_TYPES = "ALL";

    private CountdownService countdownService = mock(CountdownService.class);
    private TableLobbyService tableService = mock(TableLobbyService.class);
    private GameAvailabilityService underTest = new GameAvailabilityService(countdownService, tableService);
    private Map<String, Long> countdowns = new HashMap<String, Long>();

    @Before
    public void setUp() {
        when(countdownService.findAll()).thenReturn(countdowns);
    }

    @Test
    public void getAvailabilityOfGameTypeShouldReturnDISABLEDWhenGameDisabled() {
        when(tableService.isGameTypeAvailable(GAME_TYPE)).thenReturn(false);

        assertThat(underTest.getAvailabilityOfGameType(GAME_TYPE).getAvailability(), equalTo(GameAvailabilityService.Availability.DISABLED));
    }

    @Test
    public void getAvailabilityOfGameTypeShouldReturnMaintenanceScheduledWhenGameEnabledAndCountdownExistsForGameType() {
        when(tableService.isGameTypeAvailable(GAME_TYPE)).thenReturn(true);
        setCountdownWithId(10L, GAME_TYPE);

        assertThat(underTest.getAvailabilityOfGameType(GAME_TYPE).getAvailability(), equalTo(GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED));
    }

    @Test
    public void getAvailabilityOfGameTypeShouldReturnMaintenanceScheduledWhenGameEnabledAndCountdownExistsForAllGameTypes() {
        when(tableService.isGameTypeAvailable(GAME_TYPE)).thenReturn(true);
        setCountdownWithId(10L, ALL_GAME_TYPES);

        assertThat(underTest.getAvailabilityOfGameType(GAME_TYPE).getAvailability(), equalTo(GameAvailabilityService.Availability.MAINTENANCE_SCHEDULED));
    }

    @Test
    public void getAvailabilityOfGameTypeShouldReturnAvailableWhenGameEnabledNoCountdownExistsForGameType() {
        when(tableService.isGameTypeAvailable(GAME_TYPE)).thenReturn(true);

        assertThat(underTest.getAvailabilityOfGameType(GAME_TYPE).getAvailability(), equalTo(GameAvailabilityService.Availability.AVAILABLE));
    }

    @Test
    public void getAvailabilityOfGameTypeShouldReturnNullCountdownWhenNoCountdownExistsForGameType() {
        assertThat(underTest.getAvailabilityOfGameType(GAME_TYPE).getMaintenanceStartsAtMillis(), equalTo(null));
    }

    @Test
    public void getAvailabilityOfGameTypeShouldReturnCountdownMillisWhenExistsForGameType() {
        when(tableService.isGameTypeAvailable(GAME_TYPE)).thenReturn(true);
        Long expectedMillis = 10L;
        setCountdownWithId(expectedMillis, GAME_TYPE);

        Long actualMillis = underTest.getAvailabilityOfGameType(GAME_TYPE).getMaintenanceStartsAtMillis();

        assertThat(actualMillis, equalTo(expectedMillis));
    }

    @Test
    public void getAvailabilityOfGameTypeShouldReturnCountdownMillisWhenExistsForAllGameTypes() {
        when(tableService.isGameTypeAvailable(GAME_TYPE)).thenReturn(true);
        Long expectedMillis = 10L;
        setCountdownWithId(expectedMillis, "ALL");

        Long actualMillis = underTest.getAvailabilityOfGameType(GAME_TYPE).getMaintenanceStartsAtMillis();

        assertThat(actualMillis, equalTo(expectedMillis));
    }

    @Test
    public void getAvailabilityOfGameTypeShouldReturnEarlierCountdownMillisWhenExistsForGameTypeAndAllGameTypes() {
        when(tableService.isGameTypeAvailable(GAME_TYPE)).thenReturn(true);
        Long expectedMillis = 10L;
        setCountdownWithId(expectedMillis, "ALL");
        setCountdownWithId(expectedMillis + 1, GAME_TYPE);

        assertThat(underTest.getAvailabilityOfGameType(GAME_TYPE).getMaintenanceStartsAtMillis(), equalTo(expectedMillis));

        setCountdownWithId(expectedMillis, GAME_TYPE);
        setCountdownWithId(expectedMillis + 1, "ALL");

        assertThat(underTest.getAvailabilityOfGameType(GAME_TYPE).getMaintenanceStartsAtMillis(), equalTo(expectedMillis));
    }

    private void setCountdownWithId(Long expectedMillis, String countdownId) {
        countdowns.put(countdownId, expectedMillis);
    }
}
