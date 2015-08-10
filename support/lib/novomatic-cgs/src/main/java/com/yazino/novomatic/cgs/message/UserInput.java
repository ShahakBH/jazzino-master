package com.yazino.novomatic.cgs.message;

import java.util.HashMap;
import java.util.Map;

public class UserInput {
    private final byte[] internalState;
    private final String event;

    public UserInput(byte[] internalState, String event) {
        this.internalState = internalState;
        this.event = event;
    }

    public Map toMap() {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("type", "req_gmengine_input");
        request.put("gmstate", internalState);
        Map<String, Object> event = new HashMap<String, Object>();
        event.put("type", "evt_button_push");
        event.put("button", this.event);
        request.put("event", event);
        return request;
    }
}
