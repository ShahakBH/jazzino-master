package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.GamblerInfo;
import com.yazino.novomatic.cgs.NovomaticEvent;

import java.util.Map;

public class GamblerInfoConverter implements NovomaticRecordConverter {
    @Override
    public NovomaticEvent convert(Map<String, Object> record) {
        return new GamblerInfo((String) record.get("history"), getSafeSymbol(record), (Long) record.get("winmeter"), (Long) record.get("step"));
    }

    private String getSafeSymbol(final Map<String, Object> record) {
        final String symbol = (String) record.get("symbol");

        if (symbol.equals("\u0000")) {
            return "E";
        } else {
            return symbol;
        }
    }
}
