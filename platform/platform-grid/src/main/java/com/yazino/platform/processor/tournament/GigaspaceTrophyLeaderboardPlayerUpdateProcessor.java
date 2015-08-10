package com.yazino.platform.processor.tournament;

import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.messaging.host.TrophyLeaderboardPlayerUpdateDocument;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardPlayerUpdateRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboardPlayerUpdateResult;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
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
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 5)
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class GigaspaceTrophyLeaderboardPlayerUpdateProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceTrophyLeaderboardPlayerUpdateProcessor.class);

    private static final TrophyLeaderboardPlayerUpdateRequest TEMPLATE = new TrophyLeaderboardPlayerUpdateRequest();

    private final TrophyLeaderboardRepository trophyLeaderboardRepository;
    private final HostDocumentDispatcher hostDocumentDispatcher;
    private final DestinationFactory destinationFactory;

    public GigaspaceTrophyLeaderboardPlayerUpdateProcessor() {
        // CGLib constructor
        trophyLeaderboardRepository = null;
        hostDocumentDispatcher = null;
        destinationFactory = null;
    }

    @Autowired
    public GigaspaceTrophyLeaderboardPlayerUpdateProcessor(final TrophyLeaderboardRepository trophyLeaderboardRepository,
                                                           final HostDocumentDispatcher hostDocumentDispatcher,
                                                           final DestinationFactory destinationFactory) {
        notNull(trophyLeaderboardRepository, "trophyLeaderboardRepository may not be null");
        notNull(hostDocumentDispatcher, "hostDocumentDispatcher may not be null");
        notNull(destinationFactory, "destinationFactory may not be null");

        this.trophyLeaderboardRepository = trophyLeaderboardRepository;
        this.hostDocumentDispatcher = hostDocumentDispatcher;
        this.destinationFactory = destinationFactory;
    }

    @EventTemplate
    public TrophyLeaderboardPlayerUpdateRequest eventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public TrophyLeaderboardPlayerUpdateRequest process(final TrophyLeaderboardPlayerUpdateRequest request) {
        LOG.debug("Processing {}", request);

        if (request == null) {
            return null;
        }

        try {
            checkInitialisation();

            TrophyLeaderboard trophyLeaderboard = trophyLeaderboardRepository.findById(request.getTrophyLeaderboardId());
            if (trophyLeaderboard == null) {
                LOG.error("Trophy Leaderboard {} does not exist; cannot process request {}", request.getTrophyLeaderboardId(), request);
                return null;
            }

            trophyLeaderboard = trophyLeaderboardRepository.lock(request.getTrophyLeaderboardId());
            final TrophyLeaderboardPlayerUpdateResult updateResult = trophyLeaderboard.update(request);
            trophyLeaderboardRepository.save(trophyLeaderboard);

            hostDocumentDispatcher.send(new TrophyLeaderboardPlayerUpdateDocument(
                    updateResult, destinationFactory.player(request.getPlayerId())));

        } catch (ConcurrentModificationException e) {
            LOG.info("Unable to lock trophy leaderboard {}, retrying", request.getTrophyLeaderboardId());
            return request;

        } catch (Exception e) {
            LOG.error("Failed to process request {}", request, e);
        }

        return null;
    }

    private void checkInitialisation() {
        if (trophyLeaderboardRepository == null
                || hostDocumentDispatcher == null
                || destinationFactory == null) {
            throw new IllegalStateException("This is an uninitialised processor and cannot be called directly");
        }
    }

}
