package com.yazino.platform.repository.table;

import com.yazino.platform.persistence.table.JDBCGameConfigurationDAO;
import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.GameConfigurationProperty;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class DefaultGameConfigurationRepositoryTest {

    private static final String GAME_ID = "GAME_ID";
    private static final BigDecimal GAME_CONFIGURATION_PROPERTY_ONE = BigDecimal.valueOf(-1);

    private static final GameConfigurationProperty GCP = new GameConfigurationProperty(GAME_CONFIGURATION_PROPERTY_ONE, GAME_ID, "aPropertyName", "aPropertyValue");
    private static final GameConfiguration GC = new GameConfiguration(GAME_ID, "shortName", "displayName", new HashSet<String>(Arrays.asList("g1", "ag1")), 0).withProperties(Arrays.asList(GCP));

    @Mock
    private JDBCGameConfigurationDAO dao;


    private DefaultGameConfigurationRepository underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        underTest = new DefaultGameConfigurationRepository(dao);
    }

    @Test
    public void shouldFindByGameId() throws Exception {
        when(dao.retrieveAll()).thenReturn(new HashSet<GameConfiguration>(Arrays.asList(GC)));
        GameConfiguration gameConfiguration = underTest.findById(GAME_ID);
        assertEquals(GC, gameConfiguration);
    }

    @Test
    public void shouldRefreshAllAfterChanges() throws Exception {
        when(dao.retrieveAll()).thenReturn(new HashSet<GameConfiguration>(Arrays.asList(GC)));
        underTest.refreshAll();
        final GameConfiguration updatedGC = new GameConfiguration(GAME_ID, "newShortName", "displayName", new HashSet<String>(Arrays.asList("g1", "ag1")), 0);
        when(dao.retrieveAll()).thenReturn(new HashSet<GameConfiguration>(Arrays.asList(updatedGC)));
        underTest.refreshAll();
        GameConfiguration updatedGameConfiguration = underTest.findById(GAME_ID);
        assertEquals("newShortName", updatedGameConfiguration.getShortName());
    }

}
