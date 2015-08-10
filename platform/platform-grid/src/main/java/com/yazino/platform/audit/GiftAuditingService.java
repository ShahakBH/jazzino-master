package com.yazino.platform.audit;

import com.yazino.platform.event.message.GiftCollectedEvent;
import com.yazino.platform.event.message.GiftSentEvent;
import com.yazino.platform.gifting.CollectChoice;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static org.apache.commons.lang.Validate.notNull;

@Service
public class GiftAuditingService {
    private static final Logger LOG = LoggerFactory.getLogger(GiftAuditingService.class);

    private final QueuePublishingService<GiftSentEvent> giftSentEventQueue;
    private final QueuePublishingService<GiftCollectedEvent> giftCollectedEventQueue;

    @Autowired
    public GiftAuditingService(@Qualifier("giftSentEventQueuePublishingService") final QueuePublishingService<GiftSentEvent> giftSentEventQueue,
                               @Qualifier("giftCollectedEventQueuePublishingService") final QueuePublishingService<GiftCollectedEvent> giftCollectedEventQueue) {
        notNull(giftSentEventQueue);
        notNull(giftCollectedEventQueue);

        this.giftSentEventQueue = giftSentEventQueue;
        this.giftCollectedEventQueue = giftCollectedEventQueue;
    }

    public void auditGiftCollected(final BigDecimal giftId,
                                   final CollectChoice choice,
                                   final BigDecimal giftWinnings,
                                   final BigDecimal sessionId,
                                   final DateTime now) {
        try {
            giftCollectedEventQueue.send(new GiftCollectedEvent(giftId, choice, giftWinnings, sessionId, now));
        } catch (Exception e) {
            LOG.error("could not add gift collected event to queue", e);
        }
    }

    public void auditGiftSent(final BigDecimal giftId,
                              final BigDecimal sender,
                              final BigDecimal receiver,
                              final DateTime expiry,
                              final DateTime now,
                              final BigDecimal sessionId) {
        try {
            giftSentEventQueue.send(new GiftSentEvent(giftId, sender, receiver, expiry, now, sessionId));
        } catch (Exception e) {
            LOG.error("could not add gift sent event to queue", e);
        }
    }

}
