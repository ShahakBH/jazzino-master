package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.GameParameters;
import com.yazino.novomatic.cgs.NovomaticEvent;
import com.yazino.novomatic.cgs.SelectableValue;

import java.util.Map;

public class GameParametersConverter implements NovomaticRecordConverter {

    @Override
    public NovomaticEvent convert(Map<String, Object> record) {
        final SelectableValue lines = SelectableValue.fromMap((Map<String, Object>) record.get("lines"));
        final SelectableValue betPlacement = SelectableValue.fromMap((Map<String, Object>) record.get("betpl"));
        final SelectableValue denominationInfo = SelectableValue.fromMap((Map<String, Object>) record.get("denom"));
        return new GameParameters(lines, betPlacement, denominationInfo, (Long) record.get("credit"), (Long) record.get("wallet"));
    }
}
