package com.yazino.novomatic.cgs;

import com.yazino.novomatic.cgs.message.conversion.*;

public enum NovomaticEventType implements NovomaticEvent {

    EventGameStart("evt_default_start", Constants.RECORD_TYPE_CONVERTER),
    EventGameEnd("evt_default_end", Constants.RECORD_TYPE_CONVERTER),
    EventParametersChange("evt_chng_params", new GameParametersConverter()),
    EventReelsRotate("evt_reels_rotate", new ReelsRotateConverter()),
    EventCreditChange("evt_chng_credit", new CreditChangeConverter()),
    EventGamblerStart("evt_gambler_start", Constants.RECORD_TYPE_CONVERTER),
    EventCreditWin("evt_win_credit", new CreditWinConverter()),
    EventGamblerInfo("evt_gambler_info", new GamblerInfoConverter()),
    EventGamblerEnd("evt_gambler_end", new GamblerEndConverter());

    private final String novomaticRecordType;
    private final NovomaticRecordConverter recordTypeConverter;

    NovomaticEventType(String novomaticRecordType, NovomaticRecordConverter recordTypeConverter) {
        this.novomaticRecordType = novomaticRecordType;
        this.recordTypeConverter = recordTypeConverter;
    }

    @Override
    public String getNovomaticEventType() {
        return novomaticRecordType;
    }

    public NovomaticRecordConverter getConverter() {
        return recordTypeConverter;
    }

    public static NovomaticEventType fromNovomaticType(String type) {
        for (NovomaticEventType recordType : values()) {
            if (recordType.getNovomaticEventType().equals(type)) {
                return recordType;
            }
        }
        throw new IllegalArgumentException("Could not resolve record type for " + type);
    }

    private static class Constants {
        private static final RecordTypeConverter RECORD_TYPE_CONVERTER = new RecordTypeConverter();
    }
}
