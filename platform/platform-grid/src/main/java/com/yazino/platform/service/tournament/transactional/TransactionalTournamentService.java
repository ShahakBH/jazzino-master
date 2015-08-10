package com.yazino.platform.service.tournament.transactional;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.tournament.TournamentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Delegate class to handle inability to make GS service as @Transactional.
 */
@Service
public class TransactionalTournamentService {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionalTournamentService.class);

    private final TournamentRepository tournamentRepository;

    @SuppressWarnings("UnusedDeclaration")
    TransactionalTournamentService() {
        // CGLib constructor

        this.tournamentRepository = null;
    }

    @Autowired
    public TransactionalTournamentService(@Qualifier("tournamentRepository") final TournamentRepository tournamentRepository) {
        notNull(tournamentRepository, "tournamentRepository may not be null");

        this.tournamentRepository = tournamentRepository;
    }

    private void verifyInitialisation() {
        if (tournamentRepository == null) {
            throw new IllegalStateException(
                    "Class was created with the CGLib constructor and is invalid for direct use");
        }
    }

    @Transactional("spaceTransactionManager")
    public boolean cancelTournament(final BigDecimal toCancel) {
        verifyInitialisation();

        notNull(toCancel, "toCancel must not be null");

        try {
            final Tournament tournament = tournamentRepository.lock(toCancel);
            tournament.setTournamentStatus(TournamentStatus.CANCELLING);
            tournamentRepository.nonPersistentSave(tournament);
            return true;

        } catch (Exception e) {
            LOG.error(String.format("Failed to cancel tournament [%s]", toCancel), e);
            return false;
        }
    }

}
