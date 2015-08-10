package com.yazino.platform.service.tournament;

import com.gigaspaces.client.WriteModifiers;
import com.google.common.base.Function;
import com.j_spaces.core.LeaseContext;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.conversion.TournamentDetailTransformer;
import com.yazino.platform.model.conversion.TournamentMonitorViewTransformer;
import com.yazino.platform.model.conversion.TournamentViewFactory;
import com.yazino.platform.model.tournament.*;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.repository.tournament.TournamentScheduleRepository;
import com.yazino.platform.repository.tournament.TournamentSummaryRepository;
import com.yazino.platform.service.tournament.transactional.TransactionalTournamentService;
import com.yazino.platform.tournament.*;
import net.jini.core.lease.Lease;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingTournamentService implements TournamentService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingTournamentService.class);

    private static final TournamentMonitorViewTransformer MONITOR_VIEW_TRANSFORMER
            = new TournamentMonitorViewTransformer();
    private static final int GIGASPACE_TIMEOUT = 5000;

    private final TournamentRepository tournamentRepository;
    private final TournamentSummaryRepository tournamentSummaryRepository;
    private final SequenceGenerator sequenceGenerator;
    private final TransactionalTournamentService transactionalTournamentService;
    private final TournamentViewFactory tournamentViewFactory;
    private final TournamentDetailTransformer genericDetailTransformer;
    private final GigaSpace gigaSpace;
    private final TournamentScheduleRepository tournamentScheduleRepository;

    @Autowired(required = true)
    public GigaspaceRemotingTournamentService(
            final TournamentRepository tournamentRepository,
            final TournamentSummaryRepository tournamentSummaryRepository,
            final SequenceGenerator sequenceGenerator,
            final TransactionalTournamentService transactionalTournamentService,
            final PlayerRepository playerRepository,
            @Qualifier("tournamentViewFactory") final TournamentViewFactory tournamentViewFactory,
            final TournamentScheduleRepository tournamentScheduleRepository,
            @Qualifier("gigaSpace") final GigaSpace gigaSpace) {
        notNull(tournamentRepository, "tournamentRepository may not be null");
        notNull(tournamentSummaryRepository, "tournamentSummaryRepository may not be null");
        notNull(sequenceGenerator, "sequenceGenerator may not be null");
        notNull(transactionalTournamentService, "transactionalTournamentService may not be null");
        notNull(playerRepository, "playerRepository may not be null");
        notNull(tournamentViewFactory, "tournamentViewFactory may not be null");
        notNull(tournamentScheduleRepository, "tournamentScheduleRepository may not be null");
        notNull(gigaSpace, "gigaSpace may not be null");

        this.tournamentRepository = tournamentRepository;
        this.tournamentSummaryRepository = tournamentSummaryRepository;
        this.sequenceGenerator = sequenceGenerator;
        this.transactionalTournamentService = transactionalTournamentService;
        this.tournamentViewFactory = tournamentViewFactory;
        this.tournamentScheduleRepository = tournamentScheduleRepository;
        this.gigaSpace = gigaSpace;

        this.genericDetailTransformer = new TournamentDetailTransformer(playerRepository, null);
    }

    @Override
    public BigDecimal createTournament(final TournamentDefinition tournamentDefinition)
            throws TournamentException {
        notNull(tournamentDefinition, "tournamentDefinition may not be null");

        LOG.debug("Creating tournament: {}", tournamentDefinition);

        final Tournament tournament = new Tournament(tournamentDefinition);

        tournament.validateNewTournament();

        if (tournament.getTournamentVariationTemplate().getTournamentType() == TournamentType.PRESET
                && tournament.getStartTimeStamp() == null) {
            LOG.debug("Preset tournaments must have a start time: {}", tournament);
            throw new TournamentException(TournamentOperationResult.UNKNOWN);
        }

        tournament.setTournamentId(sequenceGenerator.next());
        tournament.setPot(BigDecimal.ZERO);
        tournament.setTournamentStatus(TournamentStatus.ANNOUNCED);

        tournamentRepository.save(tournament, true);

        LOG.debug("Created tournament {}", tournament);

        return tournament.getTournamentId();
    }


    public Set<Tournament> findByPlayer(final BigDecimal playerId) {
        notNull(playerId, "Player ID may not be null");

        LOG.debug("Looking up tournaments for player {}", playerId);

        return tournamentRepository.findByPlayer(playerId);
    }


    @Override
    public PagedData<TournamentMonitorView> findByStatus(final TournamentStatus status,
                                                         final int page) {
        notNull(status, "Status may not be null");

        LOG.debug("Looking up tournaments for status {}", status);

        final PagedData<Tournament> tournaments = tournamentRepository.findByStatus(status, page);
        if (tournaments == null) {
            return PagedData.empty();
        }

        return new PagedData<TournamentMonitorView>(tournaments.getStartPosition(), tournaments.getSize(),
                tournaments.getTotalSize(), collect(tournaments, MONITOR_VIEW_TRANSFORMER));
    }


    @Override
    public PagedData<TournamentMonitorView> findAll(final int page) {
        LOG.debug("Finding all loaded tournaments");

        final PagedData<Tournament> tournaments = tournamentRepository.findAll(page);
        if (tournaments == null) {
            return PagedData.empty();
        }

        return new PagedData<TournamentMonitorView>(tournaments.getStartPosition(),
                tournaments.getSize(), tournaments.getTotalSize(),
                collect(tournaments, MONITOR_VIEW_TRANSFORMER));
    }

    @Override
    public boolean cancelTournament(@Routing final BigDecimal toCancel) {
        notNull(toCancel, "toCancel may not be null");

        return transactionalTournamentService.cancelTournament(toCancel);
    }

    @Override
    public void populateSpaceWithNonClosedTournaments() {
        LOG.debug("Populating space with non-closed tournaments & their tournament players");

        tournamentRepository.loadNonClosedTournamentsIntoSpace();
    }

    @Override
    public void clearSpace() {
        LOG.debug("Clearing space of tournaments & tournament players");

        tournamentRepository.clear();
    }

    @Override
    public void saveRecurringTournamentDefinition(final RecurringTournament recurringTournament) {
        notNull(recurringTournament, "recurringTournament may not be null");

        final RecurringTournamentDefinition definition = new RecurringTournamentDefinition();
        definition.setEnabled(recurringTournament.isEnabled());
        if (recurringTournament.getExclusionPeriods() != null) {
            definition.setExclusionPeriods(recurringTournament.getExclusionPeriods().toArray(
                    new DayPeriod[recurringTournament.getExclusionPeriods().size()]));
        }
        definition.setFrequency(recurringTournament.getFrequency());
        definition.setId(recurringTournament.getId());
        definition.setInitialSignupTime(recurringTournament.getInitialSignupTime());
        definition.setPartnerId(recurringTournament.getPartnerId());
        definition.setSignupPeriod(recurringTournament.getSignupPeriod());
        definition.setTournamentDescription(recurringTournament.getTournamentDescription());
        definition.setTournamentName(recurringTournament.getTournamentName());
        definition.setTournamentVariationTemplate(recurringTournament.getTournamentVariationTemplate());

        tournamentRepository.save(definition);
    }

    @Override
    public TournamentView findViewById(final BigDecimal tournamentId) {
        notNull(tournamentId, "tournamentId is required");

        final Tournament tournament = tournamentRepository.findById(tournamentId);
        if (tournament == null) {
            return null;
        }

        return tournamentViewFactory.create(tournament);
    }

    @Override
    public TournamentDetail findDetailById(final BigDecimal tournamentId) {
        notNull(tournamentId, "tournamentId is required");

        final Tournament tournament = tournamentRepository.findById(tournamentId);
        if (tournament == null) {
            return null;
        }

        return genericDetailTransformer.apply(tournament);
    }

    @Override
    public Summary findLastTournamentSummary(final String gameType) {
        notNull(gameType, "gameType may not be null");

        final TournamentSummary mostRecent = tournamentSummaryRepository.findMostRecent(gameType);
        if (mostRecent != null) {
            return mostRecent.asSummary();
        }
        return null;
    }

    @Override
    public Schedule getTournamentSchedule(@Routing final String gameType) {
        notNull(gameType, "gameType may not be null");

        final TournamentSchedule tournamentSchedule = tournamentScheduleRepository.findByGameType(gameType);
        if (tournamentSchedule != null) {
            return tournamentSchedule.asSchedule();
        }
        return null;
    }

    @Override
    public TournamentOperationResult register(@Routing final BigDecimal tournamentId,
                                              final BigDecimal playerId,
                                              final boolean async) {
        notNull(tournamentId, "tournamentId may not be null");
        notNull(tournamentId, "playerId may not be null");

        LOG.debug("Registering {} on tournament {}", playerId, tournamentId);

        return sendPlayerProcessingRequest(tournamentId, playerId, TournamentPlayerProcessingType.ADD, async);
    }

    @Override
    public TournamentOperationResult deregister(@Routing final BigDecimal tournamentId,
                                                final BigDecimal playerId,
                                                final boolean async) {
        notNull(tournamentId, "tournamentId may not be null");
        notNull(tournamentId, "playerId may not be null");

        LOG.debug("Unregistering {} from tournament {}", playerId, tournamentId);

        return sendPlayerProcessingRequest(tournamentId, playerId, TournamentPlayerProcessingType.REMOVE, async);
    }

    @Override
    public Set<BigDecimal> findTableIdsFor(@Routing final BigDecimal tournamentId) {
        notNull(tournamentId, "tournamentId may not be null");

        final Tournament tournament = tournamentRepository.findById(tournamentId);
        if (tournament != null) {
            return new HashSet<BigDecimal>(tournament.getTables());
        }
        return Collections.emptySet();
    }

    private TournamentOperationResult sendPlayerProcessingRequest(final BigDecimal tournamentId,
                                                                  final BigDecimal playerId,
                                                                  final TournamentPlayerProcessingType processingType,
                                                                  final boolean async) {
        notNull(tournamentId, "Tournament ID may not be null");
        notNull(playerId, "Player ID may not be null");
        notNull(processingType, "Processing type may not be null");

        final TournamentPlayerProcessingRequest request = new TournamentPlayerProcessingRequest(
                playerId, tournamentId, processingType, async);
        if (async) {
            gigaSpace.write(request);
            return null;

        } else {
            final LeaseContext<TournamentPlayerProcessingRequest> leaseContext
                    = gigaSpace.write(request, Lease.FOREVER, GIGASPACE_TIMEOUT, WriteModifiers.WRITE_ONLY);

            final TournamentPlayerProcessingResponse response = gigaSpace.take(
                    templateFor(tournamentId, leaseContext), GIGASPACE_TIMEOUT);

            LOG.debug("Received tournament player processing response {}", response);

            if (response == null || response.getTournamentOperationResult() == null) {
                return TournamentOperationResult.NO_RESPONSE_RETURNED;
            }
            return response.getTournamentOperationResult();
        }
    }

    private <I, O> List<O> collect(final Iterable<I> toCollect,
                                   final Function<I, O> transformer) {
        if (toCollect == null) {
            return null;
        }
        final List<O> output = new ArrayList<O>();
        for (I toTransform : toCollect) {
            output.add(transformer.apply(toTransform));
        }
        return output;
    }

    private TournamentPlayerProcessingResponse templateFor(
            final BigDecimal tournamentId,
            final LeaseContext<TournamentPlayerProcessingRequest> leaseContext) {
        final TournamentPlayerProcessingResponse templateResponse = new TournamentPlayerProcessingResponse();
        templateResponse.setRequestSpaceId(leaseContext.getUID());
        templateResponse.setTournamentId(tournamentId);
        return templateResponse;
    }
}
