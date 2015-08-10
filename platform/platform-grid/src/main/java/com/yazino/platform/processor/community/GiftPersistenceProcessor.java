package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.Gift;
import com.yazino.platform.model.community.GiftPersistenceRequest;
import com.yazino.platform.persistence.community.GiftDAO;
import com.yazino.platform.repository.community.GiftRepository;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This class will process {@link com.yazino.platform.model.community.PlayerTrophyPersistenceRequest} objects that appear in GigaSpace.
 * It will use the underlying {@link com.yazino.platform.persistence.community.PlayerTrophyDAO} to do the persistence.
 */
@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class GiftPersistenceProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(GiftPersistenceProcessor.class);

    private final GiftRepository giftRepository;
    private final GiftDAO giftDao;

    @Autowired
    public GiftPersistenceProcessor(final GiftDAO giftDao,
                                    final GiftRepository giftRepository) {
        notNull(giftDao, "giftDao may not be null");
        notNull(giftRepository, "giftRepository may not be null");

        this.giftDao = giftDao;
        this.giftRepository = giftRepository;
    }

    @EventTemplate
    public GiftPersistenceRequest template() {
        return new GiftPersistenceRequest();
    }

    @SpaceDataEvent
    public void process(final GiftPersistenceRequest request) {
        LOG.debug("Processing request: {}", request);

        if (request == null) {
            return;
        }

        try {
            final Gift gift = giftRepository.findByRecipientAndId(request.getRecipientPlayerId(), request.getGiftId());
            if (gift == null) {
                LOG.warn("Gift does not exist: {}", request.getGiftId());
                return;
            }

            giftDao.save(gift);

        } catch (Throwable e) {
            LOG.error("Save failed for gift request {}", request, e);
        }
    }

}
