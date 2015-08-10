package com.yazino.novomatic.cgs.message;

import com.yazino.novomatic.cgs.NovomaticGameState;
import com.yazino.novomatic.cgs.NovomaticEvent;
import com.yazino.novomatic.cgs.message.conversion.GameStateBuilder;
import org.junit.Test;

import java.util.*;

import static junit.framework.Assert.assertEquals;

public class GameStateBuilderTest {

    @Test
    public void shouldBuildFromMap() {
//        {events=[{type=evt_default_start}, {denom={allowed=[10], value=10, type=avalue}, lines={allowed=[1], value=9, type=avalue}, credit=0, type=evt_chng_params, betpl={allowed=[1], value=1, type=avalue}, wallet=3}, {reels=[WRG, WBG, WBG, REL, RCL], type=evt_reels_rotate}, {type=evt_default_end}, {type=evt_default_start}], type=rsp_gmengine_init, gmstate=[B@1460d0eb}
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", "rsp_gmengine_init");
        map.put("gmstate", null);
        List<Map<String, Object>> events = new ArrayList<Map<String, Object>>();
        map.put("events", events);
        final NovomaticGameState expected = new NovomaticGameState(null, Arrays.<NovomaticEvent>asList());
        final NovomaticGameState actual = new GameStateBuilder().buildFromMap(map);
        assertEquals(expected, actual);
    }

    private Map<String, Object> start() {
        final HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("type", "evt_default_start");
        return result;
    }
}
