package com.yazino.platform.processor.tournament;

import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocumentDispatcher;
import com.yazino.platform.messaging.host.NewsEventHostDocument;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PublishStatusRequest;
import com.yazino.platform.model.community.PublishStatusRequestType;
import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayerProcessingRequest;
import com.yazino.platform.model.tournament.TournamentPlayerProcessingResponse;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.tournament.TournamentException;
import com.yazino.platform.tournament.TournamentOperationResult;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Responsible for processing enqueued processing requests for tournament players.
 */
@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class TournamentPlayerProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentPlayerProcessor.class);

    private static final String NEWS_EVENT_JOIN_IMAGE = "COMPETITION_JOIN";
    private static final long RESPONSE_LEASE = 60000;
    private static final TournamentPlayerProcessingRequest TEMPLATE = new TournamentPlayerProcessingRequest();

    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentHost tournamentHost;
    private final HostDocumentDispatcher hostDocumentDispatcher;
    private final GigaSpace gigaSpace;
    private final DestinationFactory destinationFactory;

    /**
     * CGLib constructor.
     */
    TournamentPlayerProcessor() {
        tournamentHost = null;
        playerRepository = null;
        tournamentRepository = null;
        hostDocumentDispatcher = null;
        gigaSpace = null;
        destinationFactory = null;
    }

    @Autowired
    public TournamentPlayerProcessor(final TournamentHost tournamentHost,
                                     final PlayerRepository playerRepository,
                                     final HostDocumentDispatcher hostDocumentDispatcher,
                                     final TournamentRepository tournamentRepository,
                                     @Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                     final DestinationFactory destinationFactory) {
        notNull(tournamentHost, "Tournament Host may not be null");
        notNull(tournamentRepository, "Tournament Repository may not be null");
        notNull(playerRepository, "playerRepository is null");
        notNull(hostDocumentDispatcher, "hostDocumentDispatcher is null");
        notNull(gigaSpace, "gigaSpace is null");
        notNull(destinationFactory, "destinationFactory is null");

        this.tournamentHost = tournamentHost;
        this.tournamentRepository = tournamentRepository;
        this.hostDocumentDispatcher = hostDocumentDispatcher;
        this.playerRepository = playerRepository;
        this.gigaSpace = gigaSpace;
        this.destinationFactory = destinationFactory;
    }

    private void checkForInitialisation() {
        if (tournamentHost == null
                || hostDocumentDispatcher == null
                || playerRepository == null
                || gigaSpace == null
                || tournamentRepository == null
                || destinationFactory == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @EventTemplate
    public TournamentPlayerProcessingRequest eventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void process(final TournamentPlayerProcessingRequest request) {
        LOG.debug("Process: {}", request);

        final Player player;
        Tournament tournament;

        try {
            checkForInitialisation();
            validateRequest(request);

            player = playerRepository.findById(request.getPlayerId());
            if (player == null) {
                LOG.error("Player does not exist: {}", request.getPlayerId());
                writeResponse(request, TournamentOperationResult.UNKNOWN);
                return;
            }

            tournament = tournamentRepository.findById(request.getTournamentId());
            if (tournament == null) {
                LOG.error("Tournament does not exist: {}", request.getTournamentId());
                writeResponse(request, TournamentOperationResult.UNKNOWN);
                return;
            }

        } catch (Exception e) {
            LOG.error("Unexpected error during player processing for tournament {}, player {}",
                    request.getTournamentId(), request.getPlayerId(), e);
            writeResponse(request, TournamentOperationResult.UNKNOWN);
            return;
        }

        tournament = tournamentRepository.lock(request.getTournamentId());

        try {
            switch (request.getProcessingType()) {
                case ADD:
                    tournament.addPlayer(player, tournamentHost);
                    break;

                case REMOVE:
                    tournament.removePlayer(player, tournamentHost);
                    break;

                default:
                    LOG.error("Processing type is not recognised: {}", request.getProcessingType());
                    writeResponse(request, TournamentOperationResult.UNKNOWN);
                    return;
            }

            playerRepository.savePublishStatusRequest(
                    new PublishStatusRequest(request.getPlayerId(), PublishStatusRequestType.PLAYER_BALANCE));

            if (tournament.findPlayer(request.getPlayerId()) != null) {
                dispatchNewsEvents(player, tournament);
            }

        } catch (TournamentException e) {
            LOG.debug("Registration failed for player {}", player.getPlayerId(), e);
            writeResponse(request, e.getResult());
            return;

        } catch (Exception e) {
            LOG.error("Unexpected error during player processing for tournament {}, player {}",
                    request.getTournamentId(), request.getPlayerId(), e);
            writeResponse(request, TournamentOperationResult.UNKNOWN);
            return;
        }

        tournamentRepository.save(tournament, true);
        writeResponse(request, TournamentOperationResult.SUCCESS);
    }

    private void validateRequest(final TournamentPlayerProcessingRequest request) {
        notNull(request, "Request may not be null");

        notNull(request.getPlayerId(), "Player ID may not be null");
        notNull(request.getTournamentId(), "Tournament ID may not be null");
        notNull(request.getProcessingType(), "Processing type may not be null");
        notNull(request.isAsync(), "Async flag may not be null");
    }

    private void writeResponse(final TournamentPlayerProcessingRequest request,
                               final TournamentOperationResult result) {
        if (!request.isAsync()) {
            gigaSpace.write(new TournamentPlayerProcessingResponse(
                    request.getSpaceId(), request.getTournamentId(), result), RESPONSE_LEASE);
        }
    }

    private void dispatchNewsEvents(final Player player,
                                    final Tournament tournament) {
        final ParameterisedMessage message = new ParameterisedMessage(
                "%s has joined the \"%s\" tournament", player.getName(), tournament.getName());
        final NewsEvent event = new NewsEvent.Builder(player.getPlayerId(), message)
                .setType(NewsEventType.NEWS)
                .setImage(NEWS_EVENT_JOIN_IMAGE + "_" + tournament.getTournamentVariationTemplate().getGameType())
                .build();
        hostDocumentDispatcher.send(new NewsEventHostDocument(tournament.getPartnerId(),
                event, destinationFactory.player(player.getPlayerId())));
    }

}
