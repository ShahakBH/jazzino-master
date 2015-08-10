package com.yazino.platform.model.community;

import java.math.BigDecimal;
import java.util.Map;

public class PublishStatusRequestWithArguments extends PublishStatusRequest {
    private Map<String, Object> arguments;

    public PublishStatusRequestWithArguments() {
        super();
    }

    public PublishStatusRequestWithArguments(BigDecimal playerId, PublishStatusRequestType requestType, final Map<String, Object> arguments) {
        super(playerId, requestType);
        this.arguments = arguments;
    }


    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(final Map<String, Object> arguments) {
        this.arguments = arguments;
    }
}
