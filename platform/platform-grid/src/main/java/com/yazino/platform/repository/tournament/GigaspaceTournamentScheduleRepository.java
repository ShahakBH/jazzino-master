package com.yazino.platform.repository.tournament;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.model.tournament.TournamentSchedule;
import net.jini.core.lease.Lease;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaspaceTournamentScheduleRepository implements TournamentScheduleRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceTournamentScheduleRepository.class);

    private static final int TIMEOUT = 3000;

    private final GigaSpace gigaSpace;

    @Autowired
    public GigaspaceTournamentScheduleRepository(@Qualifier("gigaSpace") final GigaSpace gigaSpace) {
        notNull(gigaSpace, "gigaSpace may not be null");

        this.gigaSpace = gigaSpace;
    }

    @Override
    public TournamentSchedule findByGameType(final String gameType) {
        notNull(gameType, "gameType may not be null");

        final TournamentSchedule schedule = gigaSpace.readById(TournamentSchedule.class, gameType, gameType, 0, ReadModifiers.DIRTY_READ);
        if (schedule != null) {
            return schedule;
        }
        return createDefaultScheduleFor(gameType);
    }

    @Override
    public void save(final TournamentSchedule tournamentSchedule) {
        notNull(tournamentSchedule, "tournamentSchedule may not be null");

        LOG.debug("Saving tournament schedule {}", tournamentSchedule);

        gigaSpace.write(tournamentSchedule, Lease.FOREVER, TIMEOUT, WriteModifiers.UPDATE_OR_WRITE);
    }

    private TournamentSchedule createDefaultScheduleFor(final String gameType) {
        final TournamentSchedule newSchedule = new TournamentSchedule();
        newSchedule.setGameType(gameType);
        save(newSchedule);
        return newSchedule;
    }
}
