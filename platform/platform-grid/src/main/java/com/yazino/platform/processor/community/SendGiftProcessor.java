package com.yazino.platform.processor.community;

import com.yazino.platform.audit.GiftAuditingService;
import com.yazino.platform.gifting.PlayerCollectionStatus;
import com.yazino.platform.model.community.Gift;
import com.yazino.platform.model.community.SendGiftRequest;
import com.yazino.platform.repository.community.GiftRepository;
import com.yazino.platform.service.community.GiftProperties;
import org.joda.time.DateTime;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class SendGiftProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(SendGiftProcessor.class);

    private final GiftRepository giftRepository;
    private final GiftAuditingService giftAuditingService;
    private final GiftProperties giftProperties;

    @Autowired
    public SendGiftProcessor(final GiftAuditingService giftAuditingService,
                             final GiftRepository giftRepository,
                             final GiftProperties giftProperties) {
        notNull(giftAuditingService, "giftAuditingService may not be null");
        notNull(giftRepository, "giftRepository may not be null");
        notNull(giftProperties, "giftingProperties may not be null");

        this.giftAuditingService = giftAuditingService;
        this.giftRepository = giftRepository;
        this.giftProperties = giftProperties;
    }

    @EventTemplate
    public SendGiftRequest template() {
        return new SendGiftRequest();
    }

    @SpaceDataEvent
    public void process(final SendGiftRequest request) {
        LOG.debug("Processing request: {}", request);

        if (request == null) {
            return;
        }

        try {
            final DateTime now = new DateTime();
            int expiryInHours = giftProperties.expiryTimeInHours();
            final DateTime expiry = now.plusHours(expiryInHours);

            LOG.debug("Gifting player {} from {} with ID {}",
                    request.getRecipientPlayerId(), request.getSendingPlayerId(), request.getGiftId());
            giftRepository.save(aNewGiftFor(request, now, expiry));

            giftRepository.publishReceived(request.getRecipientPlayerId());
            giftAuditingService.auditGiftSent(request.getGiftId(), request.getSendingPlayerId(), request.getRecipientPlayerId(),
                    expiry, now, request.getSessionId());
            pushPlayerCollectionStatus(request.getRecipientPlayerId());

        } catch (Throwable e) {
            LOG.error("Save failed for gift request {}", request, e);
        }
    }

    private void pushPlayerCollectionStatus(final BigDecimal playerId) {
        LOG.debug("Pushing collection status for player {}", playerId);

        final int collectedToday = giftRepository.countCollectedOn(playerId, giftProperties.startOfGiftPeriod());
        final PlayerCollectionStatus playerCollectionStatus = new PlayerCollectionStatus(
                giftProperties.remainingGiftCollections(collectedToday),
                giftRepository.countAvailableForCollection(playerId));
        giftRepository.publishCollectionStatus(playerId, playerCollectionStatus);
    }

    private Gift aNewGiftFor(final SendGiftRequest request,
                             final DateTime now,
                             final DateTime expiry) {
        return new Gift(request.getGiftId(),
                request.getSendingPlayerId(),
                request.getRecipientPlayerId(),
                now,
                expiry,
                null,
                false);
    }

}
