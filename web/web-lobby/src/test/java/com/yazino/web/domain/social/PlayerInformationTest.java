package com.yazino.web.domain.social;

import com.yazino.web.util.JsonHelper;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class PlayerInformationTest {

    private JsonHelper jsonHelper;

    @Before
    public void setUp() throws Exception {
        jsonHelper = new JsonHelper();
    }

    @Test
    public void shouldSerialiseToJson() {
        final PlayerInformation before = new PlayerInformation.Builder(BigDecimal.TEN)
                .withField(PlayerInformationType.BALANCE, BigDecimal.valueOf(12.5))
                .build();
        final String jsonBefore = jsonHelper.serialize(before);
        final PlayerInformation after = jsonHelper.deserialize(PlayerInformation.class, jsonBefore);
        final String jsonAfter = jsonHelper.serialize(after);
        assertEquals(jsonBefore, jsonAfter);
        assertEquals(2, after.size());
        assertEquals(after.get("playerId"), ((BigDecimal) before.get("playerId")).intValue());
    }
    
    @Test
    public void builderShouldIgnoreNullValues(){
        final PlayerInformation info = new PlayerInformation.Builder(BigDecimal.TEN)
                .withField(PlayerInformationType.BALANCE, null)
                .build();
        assertNull(info.get(PlayerInformationType.BALANCE.getDisplayName()));
        assertFalse(info.containsKey(PlayerInformationType.BALANCE.getDisplayName()));
    }
}
