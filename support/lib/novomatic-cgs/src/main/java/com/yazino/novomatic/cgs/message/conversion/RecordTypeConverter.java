package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.NovomaticEvent;
import com.yazino.novomatic.cgs.NovomaticEventType;

import java.util.Map;

public class RecordTypeConverter implements NovomaticRecordConverter {
    @Override
    public NovomaticEvent convert(Map<String, Object> record) {
        return NovomaticEventType.fromNovomaticType((String) record.get("type"));
    }
}
