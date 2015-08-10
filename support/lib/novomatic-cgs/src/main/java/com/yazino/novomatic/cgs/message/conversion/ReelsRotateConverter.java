package com.yazino.novomatic.cgs.message.conversion;

import com.yazino.novomatic.cgs.NovomaticEvent;
import com.yazino.novomatic.cgs.ReelsRotate;

import java.util.List;
import java.util.Map;

public class ReelsRotateConverter implements NovomaticRecordConverter {
    @Override
    public NovomaticEvent convert(Map<String, Object> record) {
        return new ReelsRotate((List<String>) record.get("reels"));
    }
}
