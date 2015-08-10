package com.yazino.platform.processor.tournament;

import com.yazino.platform.event.message.LeaderboardEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardResult;
import com.yazino.platform.model.tournament.TrophyLeaderboardResultingRequest;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import com.yazino.platform.repository.session.InboxMessageRepository;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import com.yazino.platform.repository.tournament.TrophyLeaderboardResultRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.audit.AuditLabelFactory;
import com.yazino.platform.service.tournament.AwardTrophyService;
import com.yazino.platform.tournament.TrophyLeaderboardPlayerResult;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class TrophyLeaderboardResultingProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TrophyLeaderboardResultingProcessor.class);

    private static final TrophyLeaderboardResultingRequest TEMPLATE = new TrophyLeaderboardResultingRequest();

    private final TrophyLeaderboardRepository trophyLeaderboardRepository;
    private final TrophyLeaderboardResultRepository trophyLeaderboardResultRepository;
    private final TimeSource timeSource;
    private final InternalWalletService internalWalletService;
    private final PlayerRepository playerRepository;
    private final AwardTrophyService awardTrophyService;
    private final TrophyRepository trophyRepository;
    private final InboxMessageRepository inboxMessageRepository;
    private final QueuePublishingService<LeaderboardEvent> eventService;
    private final AuditLabelFactory auditor;

    /**
     * CGLib constructor.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected TrophyLeaderboardResultingProcessor() {
        this.trophyLeaderboardRepository = null;
        this.trophyLeaderboardResultRepository = null;
        this.internalWalletService = null;
        this.playerRepository = null;
        this.awardTrophyService = null;
        this.inboxMessageRepository = null;
        this.trophyRepository = null;
        this.eventService = null;
        this.auditor = null;
        this.timeSource = null;
    }

    @Autowired
    public TrophyLeaderboardResultingProcessor(
            @Qualifier("trophyLeaderboardRepository") final TrophyLeaderboardRepository trophyLeaderboardRepository,
            final TrophyLeaderboardResultRepository trophyLeaderboardResultRepository,
            final InternalWalletService internalWalletService,
            final PlayerRepository playerRepository,
            final AwardTrophyService awardTrophyService,
            final InboxMessageRepository inboxMessageRepository,
            final TrophyRepository trophyRepository,
            @Qualifier("leaderboardEventQueuePublishingService") final QueuePublishingService<LeaderboardEvent> eventService,
            @Qualifier("auditLabelFactory") final AuditLabelFactory auditor,
            final TimeSource timeSource) {
        notNull(trophyLeaderboardRepository, "Trophy Leaderboard Repository may not be null");
        notNull(trophyLeaderboardResultRepository, "Trophy Leaderboard Result Repository may not be null");
        notNull(internalWalletService, "Wallet Service may not be null");
        notNull(playerRepository, "playerRepository may not be null");
        notNull(awardTrophyService, "awardTrophyService may not be null");
        notNull(inboxMessageRepository, "inboxMessageRepository may not be null");
        notNull(trophyRepository, "Trophy Service may not be null");
        notNull(eventService, "Event Service may not be null");
        notNull(auditor, "Auditor may not be null");
        notNull(timeSource, "Time Source may not be null");

        this.trophyLeaderboardRepository = trophyLeaderboardRepository;
        this.trophyLeaderboardResultRepository = trophyLeaderboardResultRepository;
        this.internalWalletService = internalWalletService;
        this.playerRepository = playerRepository;
        this.awardTrophyService = awardTrophyService;
        this.inboxMessageRepository = inboxMessageRepository;
        this.trophyRepository = trophyRepository;
        this.eventService = eventService;
        this.auditor = auditor;
        this.timeSource = timeSource;
    }

    private void checkForInitialisation() {
        if (trophyLeaderboardRepository == null
                || trophyLeaderboardResultRepository == null
                || internalWalletService == null
                || playerRepository == null
                || awardTrophyService == null
                || eventService == null
                || inboxMessageRepository == null
                || trophyRepository == null
                || auditor == null
                || timeSource == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @EventTemplate
    public TrophyLeaderboardResultingRequest eventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void process(final TrophyLeaderboardResultingRequest request) {
        if (request == null) {
            LOG.warn("Null request");
            return;
        }

        try {
            checkForInitialisation();
            LOG.debug("Processing resulting request: {}", request);

        } catch (Throwable t) {
            LOG.error("Error processing request {}", request, t);
            return;
        }

        TrophyLeaderboard trophyLeaderboard = trophyLeaderboardRepository.findById(request.getTrophyLeaderboardId());
        if (trophyLeaderboard == null) {
            LOG.warn("Trophy leaderboard does not exist: {}", request.getTrophyLeaderboardId());
            return;
        }

        trophyLeaderboard = trophyLeaderboardRepository.lock(request.getTrophyLeaderboardId());

        if (trophyLeaderboard.getActive() == null || !trophyLeaderboard.getActive()) {
            LOG.info("Trophy Leaderboard is no longer active: {}", request.getTrophyLeaderboardId());
            return;
        }
        if (trophyLeaderboard.getCurrentCycleEnd() == null
                || trophyLeaderboard.getCurrentCycleEnd().isAfter(timeSource.getCurrentTimeStamp())) {
            LOG.info("Trophy Leaderboard is no longer active: {}", request.getTrophyLeaderboardId());
            return;
        }

        try {
            final TrophyLeaderboardResult result = trophyLeaderboard.result(new TrophyLeaderboardResultContext(
                    trophyLeaderboardResultRepository, internalWalletService,
                    playerRepository, awardTrophyService, inboxMessageRepository, trophyRepository, auditor, timeSource));

            if (trophyLeaderboard.getCurrentCycleEnd() == null) {
                trophyLeaderboardRepository.archive(trophyLeaderboard);

            } else {
                trophyLeaderboardRepository.save(trophyLeaderboard);
            }

            final Map<Integer, BigDecimal> positions = new HashMap<Integer, BigDecimal>();

            if ((result != null) && (result.getPlayerResults() != null)) {
                for (TrophyLeaderboardPlayerResult playerResult : result.getPlayerResults()) {
                    positions.put(playerResult.getPosition(), playerResult.getPlayerId());
                }
            }

            final LeaderboardEvent event = new LeaderboardEvent(trophyLeaderboard.getId(),
                    trophyLeaderboard.getGameType(),
                    trophyLeaderboard.getEndTime(),
                    positions);
            eventService.send(event);

        } catch (Throwable t) {
            LOG.error("Error processing request {}", request, t);
        }


    }
}
