package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.NovomaticEvent;

import java.util.Map;

public interface NovomaticRecordConverter {
    NovomaticEvent convert(Map<String, Object> record);
}
