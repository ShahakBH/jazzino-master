package com.yazino.web.service;

import com.yazino.platform.table.GameTypeInformation;
import com.yazino.platform.table.TableService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.GameType;

import java.math.BigDecimal;
import java.util.Collections;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerProfileCacheRemovalListenerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(15435);
    @Mock
    private PlayerProfileCacheRemoval playerProfileCacheRemoval;
    @Mock
    private TableService tableService;
    
    private PlayerProfileCacheRemovalListener underTest;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        underTest = new PlayerProfileCacheRemovalListener(tableService, playerProfileCacheRemoval);
    }
    
    @Test(expected = NullPointerException.class)
    public void theListenerCannotBeCreatedWithANullTableService() {
        new PlayerProfileCacheRemovalListener(null, playerProfileCacheRemoval);
    }
    
    @Test(expected = NullPointerException.class)
    public void theListenerCannotBeCreatedWithANullPlayerProfileCacheRemoval() {
        new PlayerProfileCacheRemovalListener(tableService, null);
    }
    
    @Test
    public void theListenerCallsTheRemovalWithAllGameTypes() {
        when(tableService.getGameTypes()).thenReturn(
                newHashSet(gameTypeOf("game1"), gameTypeOf("game2"), gameTypeOf("game3")));
        
        underTest.playerUpdated(PLAYER_ID);
        
        verify(playerProfileCacheRemoval).remove(PLAYER_ID, "game1");
        verify(playerProfileCacheRemoval).remove(PLAYER_ID, "game2");
        verify(playerProfileCacheRemoval).remove(PLAYER_ID, "game3");
    }
    
    private GameTypeInformation gameTypeOf(final String id) {
        return new GameTypeInformation(new GameType(id, id, Collections.<String>emptySet()), true);
    }
    
}
