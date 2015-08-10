package com.yazino.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Component("purchaseTracking")
public class PurchaseTracking {
    private final TrackingService trackingService;

    private static final String SUCCESSFUL_PURCHASE_EVENT = "Successful Purchase";

    @Autowired
    public PurchaseTracking(final TrackingService trackingService) {
        notNull(trackingService, "trackingService is null");
        this.trackingService = trackingService;
    }

    public void trackSuccessfulPurchase(final BigDecimal playerId) {
        trackingService.trackEvent(null, playerId, SUCCESSFUL_PURCHASE_EVENT);
    }
}
