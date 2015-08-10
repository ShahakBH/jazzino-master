package com.yazino.web.domain.world;

import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.session.PlayerLocations;
import com.yazino.web.data.BalanceSnapshotRepository;
import com.yazino.web.data.LevelRepository;
import com.yazino.web.data.LocationDetailsRepository;
import com.yazino.web.domain.LocationDetails;
import com.yazino.web.util.JsonHelper;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalPlayerDetailsJsonWorkerTest {

    public static final String GAME_TYPE = "GAME_TYPE";
    private GlobalPlayerDetailsJsonWorker worker;
    private JsonHelper jsonHelper = new JsonHelper();
    private BigDecimal playerId1 = BigDecimal.valueOf(1);
    private BigDecimal playerId2 = BigDecimal.valueOf(2);
    private BigDecimal locationId2 = BigDecimal.valueOf(12);
    private BigDecimal locationId1 = BigDecimal.valueOf(11);
    private int levelPlayer1 = 7;
    private int levelPlayer2 = 22;
    private BigDecimal balancePlayer1 = BigDecimal.valueOf(123);
    private BigDecimal balancePlayer2 = BigDecimal.valueOf(456);

    @Before
    public void setUp() throws Exception {
        final PlayerService playerService = mock(PlayerService.class);
        when(playerService.getBasicProfileInformation(playerId1)).thenReturn(createProfileInformation(playerId1));
        when(playerService.getBasicProfileInformation(playerId2)).thenReturn(createProfileInformation(playerId2));
        final LocationDetailsRepository locationDetailsRepository = mock(LocationDetailsRepository.class);
        when(locationDetailsRepository.getLocationDetails(locationId1)).thenReturn(createLocationDetails(locationId1));
        when(locationDetailsRepository.getLocationDetails(locationId2)).thenReturn(createLocationDetails(locationId2));
        final LevelRepository levelRepository = mock(LevelRepository.class);
        when(levelRepository.getLevel(playerId1, GAME_TYPE)).thenReturn(levelPlayer1);
        when(levelRepository.getLevel(playerId2, GAME_TYPE)).thenReturn(levelPlayer2);
        final BalanceSnapshotRepository balanceSnapshotRepository = mock(BalanceSnapshotRepository.class);
        when(balanceSnapshotRepository.getBalanceSnapshot(playerId1)).thenReturn(balancePlayer1);
        when(balanceSnapshotRepository.getBalanceSnapshot(playerId2)).thenReturn(balancePlayer2);
        worker = new GlobalPlayerDetailsJsonWorker(playerService, locationDetailsRepository, levelRepository, balanceSnapshotRepository);
    }

    @Test
    public void populateAndConvertPlayerDetailsForEmptyLocations() {
        HashSet<PlayerLocations> locations = new HashSet<PlayerLocations>();
        Map<BigDecimal, PlayerDetailsJson> expected = new HashMap<BigDecimal, PlayerDetailsJson>();
        assertEquals(jsonHelper.serialize(expected), worker.buildJson(locations));
    }

    @Test
    public void populateAndConvertPlayerDetailsForPlayersWithSingleLocation() {
        HashSet<PlayerLocations> locations = new HashSet<PlayerLocations>();
        locations.add(createPlayerLocation(playerId1, locationId1));
        locations.add(createPlayerLocation(playerId2, locationId2));
        Map<BigDecimal, PlayerDetailsJson> expected = new HashMap<BigDecimal, PlayerDetailsJson>();
        expected.put(playerId1, createPlayerDetailJson(playerId1, locationId1, levelPlayer1, balancePlayer1));
        expected.put(playerId2, createPlayerDetailJson(playerId2, locationId2, levelPlayer2, balancePlayer2));
        assertEquals(jsonHelper.serialize(expected), worker.buildJson(locations));
    }

    private PlayerDetailsJson createPlayerDetailJson(BigDecimal playerId, BigDecimal locationId, int level, BigDecimal balanceSnapshot) {
        return new PlayerDetailsJson.Builder(createProfileInformation(playerId), balanceSnapshot)
                .addLevel(GAME_TYPE, level)
                .addLocation(createLocationDetails(locationId))
                .build();
    }

    private LocationDetails createLocationDetails(BigDecimal locationId) {
        return new LocationDetails(locationId, "location " + locationId, GAME_TYPE);
    }

    private BasicProfileInformation createProfileInformation(BigDecimal playerId) {
        return new BasicProfileInformation(playerId, "player " + playerId, "picture " + playerId, playerId);
    }

    private PlayerLocations createPlayerLocation(BigDecimal playerId, BigDecimal locationId) {
        return new PlayerLocations(playerId, Arrays.asList(locationId));
    }
}
