package com.yazino.platform.processor.session;

import com.yazino.platform.model.session.GlobalPlayerList;
import com.yazino.platform.model.session.GlobalPlayerListUpdateRequest;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.repository.session.GlobalPlayerListRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
public class GlobalPlayerListUpdateProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalPlayerListUpdateProcessor.class);

    private final PlayerSessionRepository playerSessionRepository;
    private final GlobalPlayerListRepository globalPlayerListRepository;

    GlobalPlayerListUpdateProcessor() {
        // CGLib constructor

        this.globalPlayerListRepository = null;
        this.playerSessionRepository = null;
    }

    @Autowired
    public GlobalPlayerListUpdateProcessor(final GlobalPlayerListRepository globalPlayerListRepository,
                                           final PlayerSessionRepository playerSessionRepository) {
        this.globalPlayerListRepository = globalPlayerListRepository;
        this.playerSessionRepository = playerSessionRepository;
    }

    private boolean verifyInitialisation() {
        return globalPlayerListRepository != null
                && playerSessionRepository != null;
    }

    @EventTemplate
    public GlobalPlayerListUpdateRequest template() {
        return new GlobalPlayerListUpdateRequest();
    }

    @SpaceDataEvent
    public void process(final GlobalPlayerListUpdateRequest request) {
        if (!verifyInitialisation()) {
            LOG.error("Class was created with the CGLib constructor and is invalid for direct use");
            return;
        }

        final GlobalPlayerList globalPlayerList = globalPlayerListRepository.read();

        try {
            LOG.debug("Process LocationChangeNotification {}", request);
            boolean listHasChanged;
            final Collection<PlayerSession> sessions = playerSessionRepository.findAllByPlayer(request.getPlayerId());
            if (!sessions.isEmpty()) {
                LOG.debug("Notification for online Player sessions: {}", sessions);
                listHasChanged = globalPlayerList.playerLocationChanged(request.getPlayerId(), sessions, playerSessionRepository);
                LOG.debug("playerLocationChanged effect {}", listHasChanged);

            } else {
                LOG.debug("Notification for offline Player");
                listHasChanged = globalPlayerList.playerGoesOffline(request.getPlayerId());
                LOG.debug("playerLocationChanged effect {}", listHasChanged);
            }
            globalPlayerListRepository.save(globalPlayerList);

        } catch (Throwable t) {
            LOG.error("Error updating global player list", t);
        }
    }

}
