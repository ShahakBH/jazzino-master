package com.yazino.platform.repository.tournament;

import com.yazino.platform.model.tournament.TournamentSchedule;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaspaceTournamentInfoRepository implements TournamentInfoRepository {

    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceTournamentInfoRepository.class);

    private final GigaSpace gigaSpace;

    /**
     * @noinspection UnusedDeclaration
     */
    public GigaspaceTournamentInfoRepository() {
        gigaSpace = null;
    }

    @Autowired(required = true)
    public GigaspaceTournamentInfoRepository(@Qualifier("gigaSpace") final GigaSpace gigaSpace) {
        notNull(gigaSpace, "GigaSpace may not be null");
        this.gigaSpace = gigaSpace;
    }

    private void checkForInitialisation() {
        if (gigaSpace == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @Override
    public TournamentSchedule findTournamentSchedule(final String gameType) {
        checkForInitialisation();
        final TournamentSchedule template = new TournamentSchedule();
        template.setGameType(gameType);
        final TournamentSchedule tournamentSchedule = gigaSpace.read(template);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("TournamentSchedule was [%s] for gameType [%s]",
                    tournamentSchedule, gameType));
        }
        if (tournamentSchedule == null) {
            return template;
        } else {
            return tournamentSchedule;
        }
    }

}
