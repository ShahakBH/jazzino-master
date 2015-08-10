package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import com.yazino.platform.tournament.Schedule;
import com.yazino.platform.tournament.TournamentRegistrationInfo;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This class contains a summary of all the tournaments for a particular game.
 */
@SpaceClass
public class TournamentSchedule implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentSchedule.class);

    private static final long serialVersionUID = 6269310150700570986L;
    private static final int TWO_HOURS = 60 * 60 * 2000;

    private long tournamentRemovalTimeOut = TWO_HOURS;
    private Set<BigDecimal> tournamentsInProgress;
    private Map<BigDecimal, TournamentRegistrationInfo> tournaments;
    private String gameType;

    /**
     * Returns a start time ordered set of tournaments for the gametype
     *
     * @return a set, never null
     */
    public Set<TournamentRegistrationInfo> getChronologicalTournaments() {
        final Set<TournamentRegistrationInfo> timeSorted = buildSortedTournamentInfos();
        final Set<BigDecimal> inProg = new HashSet<>(tournamentsInProgress);
        return Sets.filter(timeSorted, new Predicate<TournamentRegistrationInfo>() {
            @Override
            public boolean apply(final TournamentRegistrationInfo tournamentRegistrationInfo) {
                return tournamentRegistrationInfo != null
                        && !inProg.contains(tournamentRegistrationInfo.getTournamentId());
            }
        });
    }

    private SortedSet<TournamentRegistrationInfo> buildSortedTournamentInfos() {
        checkInitialised();
        final SortedSet<TournamentRegistrationInfo> timeSorted = new TreeSet<>();
        timeSorted.addAll(tournaments.values());
        return timeSorted;
    }

    public Set<TournamentRegistrationInfo> getChronologicalTournaments(final BigDecimal playerId) {
        final Set<TournamentRegistrationInfo> timeSorted = buildSortedTournamentInfos();
        final Set<BigDecimal> inProg = new HashSet<>(tournamentsInProgress);
        return Sets.filter(timeSorted, new Predicate<TournamentRegistrationInfo>() {
            @Override
            public boolean apply(final TournamentRegistrationInfo tournamentRegistrationInfo) {
                if (tournamentRegistrationInfo != null) {
                    return !inProg.contains(tournamentRegistrationInfo.getTournamentId())
                            || tournamentRegistrationInfo.isRegistered(playerId);
                }
                return false;
            }
        });
    }

    /**
     * Add a tournaments info to this schedule.
     * This will usually be done when a tournament moves into the REGISTERING phase.
     *
     * @param info must not be null
     */
    public void addRegistrationInfo(final TournamentRegistrationInfo info) {
        LOG.debug("Adding Registration info [{}]", info);

        checkInitialised();
        tournaments.put(info.getTournamentId(), info);
    }

    public boolean addInProgressTournament(final TournamentRegistrationInfo registrationInfo) {
        addRegistrationInfo(registrationInfo);
        return tournamentsInProgress.add(registrationInfo.getTournamentId());
    }

    /**
     * Removes a tournaments info from this schedule.
     * This will usually be done when a tournament moves is no longer in the REGISTERING phase.
     *
     * @param tournamentId must not be null
     */
    public boolean removeRegistrationInfo(final BigDecimal tournamentId) {
        LOG.debug("Removing Registration info with tournament id [{}]", tournamentId);

        checkInitialised();
        return tournaments.remove(tournamentId) != null;
    }

    public void setGameType(final String gameType) {
        notNull(gameType, "gameType must not be null");
        this.gameType = gameType;
    }

    @SpaceId(autoGenerate = false)
    @SpaceRouting
    public String getGameType() {
        return gameType;
    }

    public Map<BigDecimal, TournamentRegistrationInfo> getTournaments() {
        return tournaments;
    }

    public void setTournaments(final Map<BigDecimal, TournamentRegistrationInfo> tournaments) {
        this.tournaments = tournaments;
    }

    public Set<BigDecimal> getTournamentsInProgress() {
        return tournamentsInProgress;
    }

    public void setTournamentsInProgress(final Set<BigDecimal> tournamentsInProgress) {
        this.tournamentsInProgress = tournamentsInProgress;
    }

    void setTournamentRemovalTimeOut(final long tournamentRemovalTimeOut) {
        this.tournamentRemovalTimeOut = tournamentRemovalTimeOut;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final TournamentSchedule rhs = (TournamentSchedule) obj;
        return new EqualsBuilder()
                .append(gameType, rhs.gameType)
                .append(tournaments, rhs.tournaments)
                .append(tournamentsInProgress, rhs.tournamentsInProgress)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(tournaments)
                .append(gameType)
                .append(tournamentsInProgress)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public Schedule asSchedule() {
        return new Schedule(buildSortedTournamentInfos(), getTournamentsInProgress());
    }

    private void checkInitialised() {
        if (tournaments == null) {
            tournaments = new ConcurrentHashMap<>();
            tournamentsInProgress = new CopyOnWriteArraySet<>();
        } else {
            cleanupOldTournaments();
        }
    }

    private void cleanupOldTournaments() {
        final Collection<TournamentRegistrationInfo> infoCollection
                = new ArrayList<>(tournaments.values());
        for (TournamentRegistrationInfo tournamentRegistrationInfo : infoCollection) {
            final DateTime startTime = tournamentRegistrationInfo.getStartTimeStamp();
            final BigDecimal tournamentId = tournamentRegistrationInfo.getTournamentId();
            if (DateTimeUtils.currentTimeMillis() > startTime.getMillis() + tournamentRemovalTimeOut) {
                tournaments.remove(tournamentId);
                tournamentsInProgress.remove(tournamentId);
            }
        }
    }
}
