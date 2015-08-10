package com.yazino.platform.processor.tournament;

import com.yazino.platform.community.Trophy;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.AwardMedalHostDocument;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.model.tournament.AwardMedalsRequest;
import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.repository.community.TrophyRepository;
import com.yazino.platform.repository.tournament.TournamentSummaryRepository;
import com.yazino.platform.service.tournament.AwardTrophyService;
import com.yazino.platform.tournament.TournamentPlayerSummary;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class AwardMedalProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AwardMedalProcessor.class);
    private static final AwardMedalsRequest TEMPLATE = new AwardMedalsRequest();

    private static final String MEDAL_NAME_TEMPLATE = "medal_%s";
    private static final int MAX_POSITION = 3;

    private final TournamentSummaryRepository tournamentSummaryRepository;
    private final TrophyRepository trophyRepository;
    private final AwardTrophyService awardTrophyService;
    private final HostDocumentDispatcher hostDocumentDispatcher;
    private final DestinationFactory destinationFactory;

    // GCLib constructor
    public AwardMedalProcessor() {
        tournamentSummaryRepository = null;
        trophyRepository = null;
        awardTrophyService = null;
        hostDocumentDispatcher = null;
        destinationFactory = null;
    }

    @Autowired(required = true)
    public AwardMedalProcessor(final TournamentSummaryRepository tournamentSummaryRepository,
                               final TrophyRepository trophyRepository,
                               final AwardTrophyService awardTrophyService,
                               final HostDocumentDispatcher hostDocumentDispatcher,
                               final DestinationFactory destinationFactory) {
        notNull(tournamentSummaryRepository, "tournamentSummaryRepository is null");
        notNull(trophyRepository, "trophyRepository is null");
        notNull(awardTrophyService, "awardTrophyService is null");
        notNull(hostDocumentDispatcher, "hostDocumentDispatcher is null");
        notNull(destinationFactory, "destinationFactory is null");

        this.tournamentSummaryRepository = tournamentSummaryRepository;
        this.trophyRepository = trophyRepository;
        this.awardTrophyService = awardTrophyService;
        this.hostDocumentDispatcher = hostDocumentDispatcher;
        this.destinationFactory = destinationFactory;
    }

    @EventTemplate
    public AwardMedalsRequest eventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void process(final AwardMedalsRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing " + ReflectionToStringBuilder.reflectionToString(request));
        }
        try {
            final TournamentSummary summary = tournamentSummaryRepository.findByTournamentId(request.getTournamentId());
            for (TournamentPlayerSummary player : summary.playerSummaries()) {
                final int playerPosition = player.getLeaderboardPosition();
                if (playerPosition <= MAX_POSITION) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Player %s finished on position %s and should get a medal",
                                player.getId(), player.getLeaderboardPosition()));
                    }
                    final String medalName = String.format(MEDAL_NAME_TEMPLATE, playerPosition);
                    final Trophy trophy = trophyRepository.findByNameAndGameType(medalName, summary.getGameType());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(String.format("Player %s will receive trophy %s", player.getId(), trophy));
                    }
                    if (trophy != null) {
                        awardTrophyService.awardTrophy(player.getId(), trophy.getId());
                        hostDocumentDispatcher.send(new AwardMedalHostDocument(trophy.getId(),
                                request.getGameType(), destinationFactory.player(player.getId())));
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Couldn't award medals!", e);
        }

    }
}
