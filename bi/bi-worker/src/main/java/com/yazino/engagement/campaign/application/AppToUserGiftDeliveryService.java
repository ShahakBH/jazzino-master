package com.yazino.engagement.campaign.application;

import com.yazino.platform.event.message.GiftSentEvent;

import com.yazino.platform.messaging.publisher.QueuePublishingService;

/**
 * Created by jrae on 03/04/2014.
 */
public class AppToUserGiftDeliveryService {
    private final QueuePublishingService<GiftSentEvent> giftDeliveryQueue;

    public AppToUserGiftDeliveryService(final QueuePublishingService<GiftSentEvent> giftDeliveryQueue) {
        this.giftDeliveryQueue = giftDeliveryQueue;
    }

    public void sendGifts(final Long campaignId, final Long promoId) {

    }
}
