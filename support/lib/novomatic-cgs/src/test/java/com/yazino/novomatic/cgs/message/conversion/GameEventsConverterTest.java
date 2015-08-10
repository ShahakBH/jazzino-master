package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yazino.novomatic.cgs.NovomaticEventType.EventGameEnd;
import static com.yazino.novomatic.cgs.NovomaticEventType.EventGameStart;
import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class GameEventsConverterTest {

    @Test
    public void shouldBuildEvents() {
        final Map<String, Object> evtDefaultStart = record("evt_default_start");
        final Map<String, Object> evtChngParams = createEvtChngParams();
        final Map<String, Object> evtReelsRotate = createEvtReelsRotate();
        final Map<String, Object> evtChngCredit = createEvtChngCredit();
        final Map<String, Object> evtWinCredit = createEvtWinCredit();
        final Map<String, Object> evtGamblerInfo = createEvtGamblerInfo();
        final Map<String, Object> evtDefaultEnd = record("evt_default_end");

        final List<Map<String, Object>> recordMap = asList(evtDefaultStart, evtChngParams, evtReelsRotate, evtChngCredit, evtWinCredit, evtGamblerInfo, evtDefaultEnd, evtDefaultStart);

        final List<NovomaticEvent> expectedResult = asList(EventGameStart,
                new GameParameters(new SelectableValue(asList(1l), 9), new SelectableValue(asList(1l), 1),  new SelectableValue(asList(10l), 10), 0l, 3l),
                new ReelsRotate(asList("WRG", "WBG", "WBG", "REL", "RCL")),
                new CreditChanged(20l, 200l),
                new CreditWon(Arrays.asList(1l, 2l, 3l), 50l, "A", 10l, 1l),
                new GamblerInfo("EEEEEEEEEE", "H", 60l, 1l),
                EventGameEnd,
                EventGameStart);

        assertEquals(expectedResult, new GameEventsConverter().convert(recordMap));
    }

    private Map<String, Object> createEvtWinCredit() {
        final HashMap<String, Object> evtCreditWin = record("evt_win_credit");
        evtCreditWin.put("positions", Arrays.asList(1l, 2l, 3l));
        evtCreditWin.put("symbol", "A");
        evtCreditWin.put("meter", 50l);
        evtCreditWin.put("value", 10l);
        evtCreditWin.put("line", 1l);
        return evtCreditWin;
    }

    private Map<String, Object> createEvtGamblerInfo() {
        final Map<String, Object> evtGamblerInfo = record("evt_gambler_info");
        evtGamblerInfo.put("history", "EEEEEEEEEE");
        evtGamblerInfo.put("symbol", "H");
        evtGamblerInfo.put("winmeter", 60l);
        evtGamblerInfo.put("step", 1l);
        return evtGamblerInfo;
    }

    private Map<String, Object> createEvtChngCredit() {
        final Map<String, Object> evtChngCredit = record("evt_chng_credit");
        evtChngCredit.put("wallet", 200l);
        evtChngCredit.put("credit", 20l);
        return evtChngCredit;
    }

    private Map<String, Object> createEvtReelsRotate() {
        final Map<String, Object> evtReelsRotate = record("evt_reels_rotate");
        evtReelsRotate.put("reels", Arrays.asList("WRG", "WBG", "WBG", "REL", "RCL"));
        return evtReelsRotate;
    }

    private Map<String, Object> createEvtChngParams() {
        final Map<String, Object> evtChngParams = record("evt_chng_params");
        evtChngParams.put("denom", avalue(10l, asList(10l)));
        evtChngParams.put("lines", avalue(9l, asList(1l)));
        evtChngParams.put("betpl", avalue(1l, asList(1l)));
        evtChngParams.put("credit", 0l);
        evtChngParams.put("wallet", 3l);
        return evtChngParams;
    }

    private HashMap<String, Object> record(String type) {
        final HashMap<String, Object> record = new HashMap<String, Object>();
        record.put("type", type);
        return record;
    }

    private Map<String, Object> avalue(Long value, List<Long> allowed) {
        final Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("type", "avalue");
        valueMap.put("value", value);
        valueMap.put("allowed", allowed);
        return valueMap;
    }
}
