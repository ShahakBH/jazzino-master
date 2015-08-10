package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.GamblerEnd;
import com.yazino.novomatic.cgs.NovomaticEvent;

import java.util.Map;

public class GamblerEndConverter implements NovomaticRecordConverter {
    @Override
    public NovomaticEvent convert(Map<String, Object> record) {
        return new GamblerEnd((Long) record.get("winmeter"), (Long) record.get("credit"));
    }
}
