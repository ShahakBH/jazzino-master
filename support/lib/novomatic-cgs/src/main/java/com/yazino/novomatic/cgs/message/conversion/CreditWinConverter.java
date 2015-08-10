package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.CreditWon;
import com.yazino.novomatic.cgs.NovomaticEvent;

import java.util.List;
import java.util.Map;

public class CreditWinConverter implements NovomaticRecordConverter {
    @Override
    public NovomaticEvent convert(Map<String, Object> record) {
        return new CreditWon((List<Long>) record.get("positions"), (Long) record.get("meter"), (String) record.get("symbol"), (Long) record.get("value"), (Long) record.get("line"));
    }
}
