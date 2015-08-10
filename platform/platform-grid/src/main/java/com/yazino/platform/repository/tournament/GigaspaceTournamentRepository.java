package com.yazino.platform.repository.tournament;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.grid.BatchQueryHelper;
import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.tournament.*;
import com.yazino.platform.persistence.tournament.TournamentDao;
import com.yazino.platform.tournament.TournamentStatus;
import net.jini.core.lease.Lease;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("tournamentRepository")
public class GigaspaceTournamentRepository implements TournamentRepository {

    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceTournamentRepository.class);

    private static final SQLQuery<Tournament> LEADERBOARD_QUERY
            = new SQLQuery<>(Tournament.class, "leaderboardUpdatesRequired = true");
    private static final TournamentIdComparator TOURNAMENT_ID_COMPARATOR = new TournamentIdComparator();
    private static final int FIVE_SECONDS = 5000;
    private static final int PAGE_SIZE = 20;

    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final Routing routing;
    private final TournamentDao tournamentDao;

    private int timeOut = FIVE_SECONDS;

    /**
     * CGLib constructor.
     *
     * @noinspection UnusedDeclaration
     */
    GigaspaceTournamentRepository() {
        this.localGigaSpace = null;
        this.globalGigaSpace = null;
        this.routing = null;
        this.tournamentDao = null;
    }

    @Autowired
    public GigaspaceTournamentRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                                         @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                         final Routing routing,
                                         @Qualifier("tournamentDao") final TournamentDao tournamentDao) {
        notNull(localGigaSpace, "localGigaSpace may not be null");
        notNull(globalGigaSpace, "globalGigaSpace may not be null");
        notNull(routing, "routing may not be null");
        notNull(tournamentDao, "tournamentDao may not be null");

        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.routing = routing;
        this.tournamentDao = tournamentDao;
    }

    public void setTimeOut(final int timeOut) {
        this.timeOut = timeOut;
    }

    private void checkForInitialisation() {
        if (localGigaSpace == null
                || globalGigaSpace == null
                || routing == null
                || tournamentDao == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    private Tournament dirtyRead(final BigDecimal tournamentId) {
        return spaceFor(tournamentId).readById(Tournament.class, tournamentId, tournamentId, 0, ReadModifiers.DIRTY_READ);
    }

    public Tournament findById(final BigDecimal tournamentId) {
        checkForInitialisation();

        notNull(tournamentId, "Tournament ID may not be null");

        LOG.debug("Entering find by id: {}", tournamentId);
        return dirtyRead(tournamentId);
    }

    @Override
    public PagedData<Tournament> findAll(final int page) {
        checkForInitialisation();

        LOG.debug("Entering find all for page {}", page);

        final Tournament[] tournaments = globalGigaSpace.readMultiple(new Tournament(), Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);
        if (tournaments == null || tournaments.length == 0) {
            return PagedData.empty();
        }

        return tournamentsForPage(page, tournaments);
    }

    @Override
    public Set<Tournament> findLocalForLeaderboardUpdates() {
        checkForInitialisation();

        LOG.debug("Entering find for leaderboard updates");

        final Tournament[] tournaments = localGigaSpace.readMultiple(LEADERBOARD_QUERY, Integer.MAX_VALUE);
        if (tournaments == null || tournaments.length == 0) {
            return Collections.emptySet();
        }

        return new HashSet<>(Arrays.asList(tournaments));
    }

    @Override
    public PagedData<Tournament> findByStatus(final TournamentStatus status, final int page) {
        checkForInitialisation();

        notNull(status, "Status may not be null");

        LOG.debug("Entering find by status: {}", status);

        final Tournament template = new Tournament();
        template.setTournamentStatus(status);

        final Tournament[] tournaments = globalGigaSpace.readMultiple(template, Integer.MAX_VALUE);
        if (tournaments == null || tournaments.length == 0) {
            return PagedData.empty();
        }

        return tournamentsForPage(page, tournaments);
    }

    private PagedData<Tournament> tournamentsForPage(final int page, final Tournament[] tournaments) {
        Arrays.sort(tournaments, TOURNAMENT_ID_COMPARATOR);

        final List<Tournament> pagedTournaments = new ArrayList<>(PAGE_SIZE);
        final int startIndex = page * PAGE_SIZE;
        for (int i = startIndex; i < startIndex + PAGE_SIZE && i < tournaments.length; ++i) {
            pagedTournaments.add(tournaments[i]);
        }

        return new PagedData<>(startIndex, pagedTournaments.size(), tournaments.length, pagedTournaments);
    }


    @Override
    public Set<Tournament> findByPlayer(final BigDecimal playerId) {
        checkForInitialisation();

        notNull(playerId, "Player ID may not be null");

        LOG.debug("Entering find by player with ID: {}", playerId);

        final TournamentPlayerInfo template = new TournamentPlayerInfo();
        template.setPlayerId(playerId);

        final TournamentPlayerInfo[] playerInfos = globalGigaSpace.readMultiple(template, Integer.MAX_VALUE);
        if (playerInfos != null) {
            final Collection<Object> tournamentIds = new ArrayList<>();
            for (final TournamentPlayerInfo playerInfo : playerInfos) {
                tournamentIds.add(playerInfo.getTournamentId());
            }

            final BatchQueryHelper queryHelper = new BatchQueryHelper(globalGigaSpace, "tournamentId");
            //noinspection unchecked
            return queryHelper.findByIds(Tournament.class, tournamentIds);
        }

        return Collections.emptySet();
    }

    public Tournament lock(final BigDecimal tournamentId) {
        checkForInitialisation();

        notNull(tournamentId, "Tournament ID may not be null");

        LOG.debug("Entering lock for tourament with ID: {}", tournamentId);

        final Tournament tournament = localGigaSpace.readById(Tournament.class, tournamentId, tournamentId, timeOut, ReadModifiers.EXCLUSIVE_READ_LOCK);
        if (tournament == null) {
            throw new ConcurrentModificationException(
                    "Cannot obtain lock, will reprocess lock for tournament: " + tournamentId);
        }

        return tournament;
    }

    public void save(final Tournament tournament, final boolean persist) {
        checkForInitialisation();

        notNull(tournament, "Tournament may not be null");

        notNull(tournament.getTournamentId(), "Tournament ID may not be null");
        notNull(tournament.getSignupStartTimeStamp(), "Tournament: Signup Start Time/Date may not be null");
        notNull(tournament.getTournamentVariationTemplate(), "Tournament: Variation Template may not be null");
        notNull(tournament.getTournamentVariationTemplate().getTournamentVariationTemplateId(),
                "Tournament: Variation Template: ID may not be null");
        notNull(tournament.getName(), "Tournament: Name may not be null");

        LOG.debug("Creating tournament {}", tournament);
        final GigaSpace spaceReference = spaceFor(tournament.getTournamentId());
        spaceReference.write(tournament, Lease.FOREVER, timeOut, WriteModifiers.UPDATE_OR_WRITE);

        if (persist) {
            final TournamentPersistenceRequest persistenceRequest = new TournamentPersistenceRequest(
                    tournament.getTournamentId());
            spaceReference.write(persistenceRequest);
        }

        generatePlayerInfo(tournament);
    }

    @Override
    public void nonPersistentSave(final Tournament tournament) {
        checkForInitialisation();

        notNull(tournament, "Tournament may not be null");

        LOG.debug("Non-persistent save for tournament {}", tournament);
        spaceFor(tournament.getTournamentId()).write(tournament, Lease.FOREVER, timeOut, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Override
    public void save(final RecurringTournamentDefinition recurringTournamentDefinition) {
        checkForInitialisation();

        notNull(recurringTournamentDefinition, "recurringTournamentDefinition may not be null");

        spaceFor(recurringTournamentDefinition.getId()).write(recurringTournamentDefinition);
    }

    private void generatePlayerInfo(final Tournament tournament) {
        final TournamentPlayerInfo playerTemplate = new TournamentPlayerInfo();
        playerTemplate.setTournamentId(tournament.getTournamentId());

        final GigaSpace spaceReference = spaceFor(tournament.getTournamentId());
        final TournamentPlayerInfo[] spacePlayers = spaceReference.readMultiple(playerTemplate, Integer.MAX_VALUE);

        final Set<TournamentPlayerInfo> currentPlayers = new HashSet<>();
        for (TournamentPlayer tournamentPlayer : tournament.tournamentPlayers()) {
            currentPlayers.add(tournamentPlayer.toTournamentPlayerInfo(tournament));
        }

        if (spacePlayers != null) {
            for (TournamentPlayerInfo spacePlayer : spacePlayers) {
                if (!currentPlayers.contains(spacePlayer)) {
                    spaceReference.clear(spacePlayer);
                }
            }
        }

        for (TournamentPlayerInfo currentPlayer : currentPlayers) {
            spaceReference.write(currentPlayer);
        }
    }

    @Override
    public void loadNonClosedTournamentsIntoSpace() {
        checkForInitialisation();

        LOG.debug("Loading non-closed tournaments into space");

        final List<Tournament> nonClosedTournaments = tournamentDao.findNonClosedTournaments();
        if (nonClosedTournaments == null) {
            return;
        }

        for (final Tournament tournament : nonClosedTournaments) {
            spaceFor(tournament.getTournamentId()).write(tournament, Lease.FOREVER, timeOut, WriteModifiers.UPDATE_OR_WRITE);
            generatePlayerInfo(tournament);
        }
    }

    @Override
    public void clear() {
        checkForInitialisation();

        LOG.debug("Clearing tournaments from space");

        globalGigaSpace.clear(new Tournament());
        globalGigaSpace.clear(new TournamentPlayerInfo());
    }

    @Override
    public void remove(final Tournament tournament) {
        checkForInitialisation();

        LOG.debug("Removing tournament {}", tournament);

        notNull(tournament, "Tournament may not be null");

        final GigaSpace spaceReference = spaceFor(tournament.getTournamentId());
        try {
            tournamentDao.save(tournament);
            spaceReference.clear(new Tournament(tournament.getTournamentId()));

        } catch (Exception e) {
            LOG.error("Tournament pre-removal persistence failed", e);
            tournament.setTournamentStatus(TournamentStatus.ERROR);

            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            tournament.setMonitoringMessage("Error: " + stringWriter.toString());

            spaceReference.write(tournament);
        }

        spaceReference.clear(new TournamentPersistenceRequest(tournament.getTournamentId()));

        final TournamentPlayerInfo playerInfoTemplate = new TournamentPlayerInfo();
        playerInfoTemplate.setTournamentId(tournament.getTournamentId());
        spaceReference.clear(playerInfoTemplate);
    }

    @Override
    public void playerEliminatedFrom(final BigDecimal tournamentId,
                                     final BigDecimal playerId,
                                     final String gameType,
                                     final int numberOfPlayers,
                                     final int leaderboardPosition) {
        notNull(tournamentId, "tournamentId may not be null");
        notNull(playerId, "playerId may not be null");
        notNull(gameType, "gameType may not be null");

        spaceFor(tournamentId).write(new TournamentPlayerEliminationRequest(tournamentId, playerId, gameType, numberOfPlayers, leaderboardPosition));
    }

    private GigaSpace spaceFor(final Object spaceId) {
        if (routing.isRoutedToCurrentPartition(spaceId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }

    private static class TournamentIdComparator implements Comparator<Tournament>, Serializable {
        private static final long serialVersionUID = -2180963904321277792L;

        @Override
        public int compare(final Tournament tournament1, final Tournament tournament2) {
            return tournament1.getTournamentId().compareTo(tournament2.getTournamentId());
        }
    }
}
