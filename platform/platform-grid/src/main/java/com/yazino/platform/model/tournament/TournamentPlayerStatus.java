package com.yazino.platform.model.tournament;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * States for tournament players.
 */
public enum TournamentPlayerStatus {

    /**
     * User has been verified and added to the tournament, but has not yet been charged.
     */
    ADDITION_PENDING("P", true, false),

    /**
     * User has been verified, charged and is ready for final addition processing..
     */
    CHARGED("C", true, false),

    /**
     * User has been verified, charged and is a valid member of the tournament.
     */
    ACTIVE("A", true, true),

    /**
     * The user has been eliminated from the tournament.
     */
    ELIMINATED("E", false, false),

    /**
     * The removal of the user is pending, waiting on a refund.
     */
    REMOVAL_PENDING("R", true, false),

    /**
     * The removal of the user is pending, the user has been refunded.
     */
    REFUNDED("F", false, false),

    /**
     * No more operations are expected for this player.
     */
    TERMINATED("T", false, false);

    static {
        setSuccessorStates(ADDITION_PENDING, CHARGED);
        setSuccessorStates(CHARGED, ACTIVE);
        setSuccessorStates(ACTIVE, REMOVAL_PENDING, ELIMINATED, TERMINATED);
        setSuccessorStates(REMOVAL_PENDING, ACTIVE, REFUNDED);
        setSuccessorStates(ELIMINATED, TERMINATED);
    }

    private final String id;
    private Set<TournamentPlayerStatus> successorStates;
    private final boolean accountRequired;
    private final boolean canHaveTable;

    private TournamentPlayerStatus(final String id,
                                   final boolean accountRequired,
                                   final boolean canHaveTable) {
        notNull(id, "ID may not be null");
        this.id = id;
        this.accountRequired = accountRequired;
        this.canHaveTable = canHaveTable;
    }

    public String getId() {
        return id;
    }

    public boolean isAccountRequired() {
        return accountRequired;
    }

    public boolean canHaveTable() {
        return canHaveTable;
    }

    private static void setSuccessorStates(final TournamentPlayerStatus tournamentPlayerStatus,
                                           final TournamentPlayerStatus... successorStates) {
        notNull(tournamentPlayerStatus, "Status may not be null");

        if (tournamentPlayerStatus.successorStates == null) {
            tournamentPlayerStatus.successorStates = new HashSet<TournamentPlayerStatus>(successorStates.length);
        } else {
            tournamentPlayerStatus.successorStates.clear();
        }

        if (successorStates != null) {
            tournamentPlayerStatus.successorStates.addAll(Arrays.asList(successorStates));
        }
    }

    public boolean isValidSuccessor(final TournamentPlayerStatus tournamentPlayerStatus) {
        notNull(tournamentPlayerStatus, "Status may not be null");

        return successorStates != null && successorStates.contains(tournamentPlayerStatus);
    }

    /**
     * Find a status by its persisted ID.
     *
     * @param id the ID.
     * @return the status.
     * @throws IllegalArgumentException if the ID is invalid.
     */
    public static TournamentPlayerStatus getById(final String id) {
        notBlank(id, "ID may not be blank");

        for (final TournamentPlayerStatus status : values()) {
            if (status.getId().equalsIgnoreCase(id)) {
                return status;
            }
        }

        throw new IllegalArgumentException("ID is invalid: " + id);
    }
}
