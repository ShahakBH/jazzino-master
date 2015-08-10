package com.yazino.web.data;

import com.yazino.platform.session.PlayerSessionStatus;
import com.yazino.platform.session.SessionService;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang.Validate.notNull;

@Repository("sessionStatusRepository")
public class SessionStatusRepository {
    private static final Logger LOG = LoggerFactory.getLogger(SessionStatusRepository.class);

    private final SessionService sessionService;
    private final Ehcache cache;

    @Autowired
    public SessionStatusRepository(final SessionService sessionService,
                                   @Qualifier("sessionStatusCache") final Ehcache cache) {
        notNull(cache, "cache is null");
        this.sessionService = sessionService;
        this.cache = cache;
    }

    public PlayerSessionStatus getStatus(final BigDecimal playerId) {
        final Element element = cache.get(playerId);
        if (element == null) {
            return null;
        }
        return (PlayerSessionStatus) element.getValue();
    }

    @SuppressWarnings("unchecked")
    public void refresh() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Refreshing SessionStatusRepository...");
        }
        final Set<BigDecimal> remainingPlayerIds = new HashSet<BigDecimal>(cache.getKeys());
        final Set<PlayerSessionStatus> sessions = sessionService.retrieveAllSessionStatuses();
        for (PlayerSessionStatus session : sessions) {
            final BigDecimal playerId = session.getPlayerId();
            cache.put(new Element(playerId, session));
            remainingPlayerIds.remove(playerId);
        }
        for (BigDecimal existingPlayerIds : remainingPlayerIds) {
            cache.remove(existingPlayerIds);
        }
    }
}
