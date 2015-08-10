package com.yazino.web.domain.social;

import com.yazino.platform.session.PlayerSessionStatus;
import com.yazino.web.data.LocationDetailsRepository;
import com.yazino.web.data.SessionStatusRepository;
import com.yazino.web.domain.LocationDetails;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocationsRetrieverTest {

    private LocationsRetriever underTest;
    private SessionStatusRepository sessionStatusRepository;
    private LocationDetailsRepository locationDetailsRepository;

    @Before
    public void setUp() throws Exception {
        sessionStatusRepository = mock(SessionStatusRepository.class);
        locationDetailsRepository = mock(LocationDetailsRepository.class);
        underTest = new LocationsRetriever(sessionStatusRepository, locationDetailsRepository);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnPlayerLocationsBasedOnTheirSessionStatus() {
        final Set<String> locationIds = new HashSet<String>(Arrays.asList("11", "22"));
        when(sessionStatusRepository.getStatus(BigDecimal.ONE)).thenReturn(new PlayerSessionStatus(locationIds, BigDecimal.ONE));
        final LocationDetails location1 = new LocationDetails(BigDecimal.valueOf(11), "table 11", "gameType");
        final LocationDetails location2 = new LocationDetails(BigDecimal.valueOf(22), "table 22", "gameType");
        when(locationDetailsRepository.getLocationDetails(BigDecimal.valueOf(11))).thenReturn(location1);
        when(locationDetailsRepository.getLocationDetails(BigDecimal.valueOf(22))).thenReturn(location2);
        final Set<LocationDetails> result = (Set<LocationDetails>) underTest.retrieveInformation(BigDecimal.ONE, "gameType");
        assertEquals(2, result.size());
        assertTrue(result.contains(location1));
        assertTrue(result.contains(location2));
    }
    
    @Test
    public void shouldFilterLocationsBasedOnTheirGameType() {
        final Set<String> locationIds = new HashSet<String>(Arrays.asList("11", "22"));
        when(sessionStatusRepository.getStatus(BigDecimal.ONE)).thenReturn(new PlayerSessionStatus(locationIds, BigDecimal.ONE));
        final LocationDetails location1 = new LocationDetails(BigDecimal.valueOf(11), "table 11", "gameType1");
        final LocationDetails location2 = new LocationDetails(BigDecimal.valueOf(22), "table 22", "gameType2");
        when(locationDetailsRepository.getLocationDetails(BigDecimal.valueOf(11))).thenReturn(location1);
        when(locationDetailsRepository.getLocationDetails(BigDecimal.valueOf(22))).thenReturn(location2);
        final Set<LocationDetails> result = (Set<LocationDetails>) underTest.retrieveInformation(BigDecimal.ONE, "gameType1");
        assertEquals(1, result.size());
        assertTrue(result.contains(location1));
    }

    @Test
    public void shouldIgnoreNonexistentLocations() {
        when(sessionStatusRepository.getStatus(BigDecimal.ONE)).thenReturn(new PlayerSessionStatus(newHashSet("11", "12"), BigDecimal.ONE));
        when(locationDetailsRepository.getLocationDetails(BigDecimal.valueOf(11))).thenReturn(null);
        when(locationDetailsRepository.getLocationDetails(BigDecimal.valueOf(22))).thenReturn(new LocationDetails(BigDecimal.valueOf(22), "table 22", "gameType2"));

        final Set<LocationDetails> result = (Set<LocationDetails>) underTest.retrieveInformation(BigDecimal.ONE, "gameType1");

        assertThat(result, is(empty()));
    }
    
    @Test
    public void shouldReturnAllIfNoFilter() {
        final Set<String> locationIds = new HashSet<String>(Arrays.asList("11", "22"));
        when(sessionStatusRepository.getStatus(BigDecimal.ONE)).thenReturn(new PlayerSessionStatus(locationIds, BigDecimal.ONE));
        final LocationDetails location1 = new LocationDetails(BigDecimal.valueOf(11), "table 11", "gameType1");
        final LocationDetails location2 = new LocationDetails(BigDecimal.valueOf(22), "table 22", "gameType2");
        when(locationDetailsRepository.getLocationDetails(BigDecimal.valueOf(11))).thenReturn(location1);
        when(locationDetailsRepository.getLocationDetails(BigDecimal.valueOf(22))).thenReturn(location2);
        final Set<LocationDetails> result = (Set<LocationDetails>) underTest.retrieveInformation(BigDecimal.ONE, null);
        assertEquals(2, result.size());
        assertTrue(result.contains(location1));
        assertTrue(result.contains(location2));
    }
    
    @Test
    public void shouldReturnNullForOfflinePlayers(){
        assertNull(underTest.retrieveInformation(BigDecimal.ONE, null));
    }
}
