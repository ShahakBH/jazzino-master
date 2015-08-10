package com.yazino.web.service;

import com.yazino.platform.Platform;

import java.math.BigDecimal;
import java.util.Map;

public interface TrackingService {

    void trackEvent(Platform platform, BigDecimal playerId, String eventName);

    void trackEvent(Platform platform, BigDecimal playerId, String eventName, Map<String, String> eventProperties);

}
