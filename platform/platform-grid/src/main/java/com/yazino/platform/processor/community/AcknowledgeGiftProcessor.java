package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.AcknowledgeGiftRequest;
import com.yazino.platform.model.community.Gift;
import com.yazino.platform.repository.community.GiftRepository;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ConcurrentModificationException;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class AcknowledgeGiftProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AcknowledgeGiftProcessor.class);

    private final GiftRepository giftRepository;

    @Autowired
    public AcknowledgeGiftProcessor(final GiftRepository giftRepository) {
        notNull(giftRepository, "giftRepository may not be null");

        this.giftRepository = giftRepository;
    }

    @EventTemplate
    public AcknowledgeGiftRequest template() {
        return new AcknowledgeGiftRequest();
    }

    @SpaceDataEvent
    public void process(final AcknowledgeGiftRequest request) {
        LOG.debug("Processing request: {}", request);

        if (request == null) {
            return;
        }

        try {
            Gift gift = giftRepository.findByRecipientAndId(request.getRecipientPlayerId(), request.getGiftId());
            if (gift == null) {
                LOG.warn("Gift {} for recipient {} does not exist", request.getGiftId(), request.getRecipientPlayerId());
                return;
            }

            gift = giftRepository.lockByRecipientAndId(request.getRecipientPlayerId(), request.getGiftId());

            gift.setAcknowledged(true);
            giftRepository.save(gift);

            LOG.debug("Gift {} has been acknowledged", request.getGiftId());

        } catch (ConcurrentModificationException e) {
            throw e;

        } catch (Throwable e) {
            LOG.error("Acknowledgement failed for gift request {}", request, e);
        }
    }

}
