package com.yazino.platform.processor.session;

import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.session.LocationChange;

import java.math.BigDecimal;
import java.util.Date;

public class PlayerSessionWorker {
    private PlayerSessionRepository repository;

    public PlayerSessionWorker(final PlayerSessionRepository repository) {
        this.repository = repository;
    }

    public PlayerSession authenticate(final BigDecimal playerId,
                                      final String localSessionKey) {
        return repository.findByPlayerAndSessionKey(playerId, localSessionKey);
    }

    public void processLocationChange(final PlayerSession session,
                                      final LocationChange notification) {
        if (session == null) {
            return;
        }

        if (notification.getSessionId() == null || notification.getSessionId().equals(session.getSessionId())) {
            updateLocationsFor(session, notification);
        }

        touch(session);
    }

    private void updateLocationsFor(final PlayerSession session, final LocationChange notification) {
        switch (notification.getType()) {
            case ADD:
                session.addLocation(notification.getLocation());
                break;
            case REMOVE:
                session.removeLocation(notification.getLocation());
                break;
            default:
                // ignored
                break;
        }
    }

    public void touch(final PlayerSession session) {
        session.setTimestamp(new Date());
    }
}
