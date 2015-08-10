package com.yazino.platform.messaging.host;

import com.yazino.game.api.ObservableChange;
import com.yazino.game.api.ObservableStatus;
import com.yazino.game.api.ObservableTimeOutEventInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ObservableStatusStub implements ObservableStatus {
    private static final long serialVersionUID = -389769301242621528L;

    private List<ObservableChange> changes = new ArrayList<ObservableChange>();

    public List<ObservableChange> getObservableChanges() {
        return changes;
    }

    public Set<String> getAllowedActions() {
        return new HashSet<String>();
    }

    public Set<String> getWarningCodes() {
        return new HashSet<String>();
    }

    public ObservableTimeOutEventInfo getNextEvent() {
        return null;
    }
}
