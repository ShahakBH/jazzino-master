package com.yazino.game.api;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public interface ObservableStatus extends Serializable {
    public enum WarningCode {
        TopupRequred,
        LastChanceToBet
    }

    List<ObservableChange> getObservableChanges();

    Set<String> getAllowedActions();

    Set<String> getWarningCodes();

    ObservableTimeOutEventInfo getNextEvent();
}
