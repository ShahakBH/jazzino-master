package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardCreationRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboardCreationResponse;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static com.yazino.platform.model.tournament.TrophyLeaderboardCreationResponse.Status.FAILURE;
import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class TrophyLeaderboardCreationProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TrophyLeaderboardCreationProcessor.class);

    private static final TrophyLeaderboardCreationRequest TEMPLATE = new TrophyLeaderboardCreationRequest();

    private final TrophyLeaderboardRepository trophyLeaderboardRepository;
    private final SequenceGenerator sequenceGenerator;

    /**
     * CGLib constructor.
     */
    protected TrophyLeaderboardCreationProcessor() {
        this.trophyLeaderboardRepository = null;
        this.sequenceGenerator = null;
    }

    @Autowired(required = true)
    public TrophyLeaderboardCreationProcessor(final TrophyLeaderboardRepository trophyLeaderboardRepository,
                                              final SequenceGenerator sequenceGenerator) {
        notNull(trophyLeaderboardRepository, "Trophy Leaderboard Repository may not be null");
        notNull(sequenceGenerator, "Sequence Generator may not be null");

        this.trophyLeaderboardRepository = trophyLeaderboardRepository;
        this.sequenceGenerator = sequenceGenerator;
    }

    private void checkForInitialisation() {
        if (trophyLeaderboardRepository == null
                || sequenceGenerator == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @EventTemplate
    public TrophyLeaderboardCreationRequest eventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public TrophyLeaderboardCreationResponse process(final TrophyLeaderboardCreationRequest request) {
        if (request == null) {
            LOG.warn("Null request");
            return null;
        }

        try {
            checkForInitialisation();

            LOG.debug("Creating trophy leaderboard: {}", request.getTrophyLeaderboard());

            final TrophyLeaderboard trophyLeaderboard = request.getTrophyLeaderboard();
            trophyLeaderboard.setId(sequenceGenerator.next());

            trophyLeaderboardRepository.save(trophyLeaderboard);

            return new TrophyLeaderboardCreationResponse(request.getSpaceId(), trophyLeaderboard.getId());

        } catch (Throwable t) {
            LOG.error("Error processing request {}", request, t);
            return new TrophyLeaderboardCreationResponse(request.getSpaceId(), FAILURE);
        }
    }
}
