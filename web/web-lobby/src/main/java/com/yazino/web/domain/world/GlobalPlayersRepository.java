package com.yazino.web.domain.world;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.session.PlayerLocations;
import com.yazino.platform.session.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("globalPlayersRepository")
public class GlobalPlayersRepository {

    private final SessionService sessionService;
    private final GlobalPlayerDetailsJsonWorker worker;

    //cglib
    public GlobalPlayersRepository() {
        this.worker = null;
        sessionService = null;
    }

    @Autowired
    public GlobalPlayersRepository(final SessionService sessionService,
                                   @Qualifier("globalPlayerDetailsJsonWorker") final GlobalPlayerDetailsJsonWorker worker) {
        notNull(sessionService, "sessionService is null");
        notNull(worker, "worker is null");

        this.sessionService = sessionService;
        this.worker = worker;
    }

    @Cacheable(cacheName = "globalPlayersCache")
    public String getPlayerLocations() {
        final Set<PlayerLocations> globalPlayerList = sessionService.getGlobalPlayerList();
        return worker.buildJson(globalPlayerList);
    }
}
