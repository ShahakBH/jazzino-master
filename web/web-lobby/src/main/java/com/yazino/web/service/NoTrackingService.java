package com.yazino.web.service;

import com.yazino.platform.Platform;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service("NoTrackingService")
public class NoTrackingService implements TrackingService {
    @Override
    public void trackEvent(final Platform platform, final BigDecimal playerId, final String eventName) {
        //do absolutely nothing
    }

    @Override
    public void trackEvent(final Platform platform,
                           final BigDecimal playerId,
                           final String eventName,
                           final Map<String, String> eventProperties) {
        //do even less than nothing.
    }
}
