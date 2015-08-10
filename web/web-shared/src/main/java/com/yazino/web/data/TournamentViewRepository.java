package com.yazino.web.data;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.tournament.TournamentService;
import com.yazino.platform.tournament.TournamentView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class TournamentViewRepository {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentViewRepository.class);

    private final TournamentService tournamentService;

    //cglib
    protected TournamentViewRepository() {
        tournamentService = null;
    }

    @Autowired
    public TournamentViewRepository(final TournamentService tournamentService) {
        notNull(tournamentService, "tournamentService is null");

        this.tournamentService = tournamentService;
    }

    @Cacheable(cacheName = "tournamentViewCache")
    public TournamentView getTournamentView(final BigDecimal tournamentId) {
        notNull(tournamentId, "tournamentId may not be null");

        try {
            return tournamentService.findViewById(tournamentId);

        } catch (Exception e) {
            LOG.error("View retrieval failed for {}", tournamentId, e);
            return null;
        }
    }
}
