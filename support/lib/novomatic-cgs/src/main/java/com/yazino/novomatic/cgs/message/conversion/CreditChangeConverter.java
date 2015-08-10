package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.CreditChanged;
import com.yazino.novomatic.cgs.NovomaticEvent;

import java.util.Map;

public class CreditChangeConverter implements NovomaticRecordConverter {
    @Override
    public NovomaticEvent convert(Map<String, Object> record) {
        return new CreditChanged((Long) record.get("credit"), (Long) record.get("wallet"));
    }
}
