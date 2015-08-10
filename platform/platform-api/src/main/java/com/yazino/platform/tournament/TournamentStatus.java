package com.yazino.platform.tournament;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

public enum TournamentStatus {
    /**
     * The tournament is now only of historical interest.
     */
    CLOSED("C", true),

    /**
     * The tournament has been settled and winnings paid out.
     */
    SETTLED("S", true),

    /**
     * Tournament event processing is finished and we are awaiting resulting.
     */
    FINISHED("F", false),

    /**
     * The tournament is waiting for game clients to finish.
     */
    WAITING_FOR_CLIENTS("W", false),

    /**
     * The tournament is waiting for tables to close at either the end of a round or of a tournament.
     */
    ON_BREAK("B", false),

    /**
     * The tournament is running a round.
     */
    RUNNING("R", false),

    /**
     * The tournament is available for signup.
     */
    REGISTERING("E", false),

    /**
     * The tournament is open and can be viewed, however signups cannot be changed.
     */
    ANNOUNCED("A", false),

    /**
     * The tournament did not meet the starting criteria (e.g. enough players) and will be cancelled.
     */
    CANCELLING("D", false),

    /**
     * The tournament did not meet the starting criteria (e.g. enough players) and has been cancelled.
     */
    CANCELLED("L", true),

    /**
     * An error happened
     */
    ERROR("X", false);

    static {
        setSuccessorStates(ANNOUNCED, REGISTERING, RUNNING, CANCELLING);
        setSuccessorStates(REGISTERING, RUNNING, ANNOUNCED, CANCELLING);
        setSuccessorStates(RUNNING, WAITING_FOR_CLIENTS, ON_BREAK);
        setSuccessorStates(WAITING_FOR_CLIENTS, ON_BREAK);
        setSuccessorStates(ON_BREAK, RUNNING, FINISHED);
        setSuccessorStates(FINISHED, SETTLED, ERROR);
        setSuccessorStates(CANCELLING, CANCELLED, ERROR);
        setSuccessorStates(SETTLED, CLOSED);
        setSuccessorStates(CANCELLED, CLOSED);
    }

    private String id;
    private Set<TournamentStatus> successorStates;
    private boolean closeAccounts;

    private TournamentStatus(final String id, final boolean closeAccounts) {
        this.closeAccounts = closeAccounts;
        notBlank(id, "ID may not be blank");

        this.id = id;
    }

    private static void setSuccessorStates(final TournamentStatus tournamentStatus,
                                           final TournamentStatus... successorStates) {
        notNull(tournamentStatus, "Status may not be null");

        if (tournamentStatus.successorStates == null) {
            tournamentStatus.successorStates = new HashSet<TournamentStatus>(successorStates.length);
        } else {
            tournamentStatus.successorStates.clear();
        }

        if (successorStates != null) {
            tournamentStatus.successorStates.addAll(Arrays.asList(successorStates));
        }
    }

    public boolean isValidSuccessor(final TournamentStatus tournamentStatus) {
        notNull(tournamentStatus, "Status may not be null");

        return successorStates != null && successorStates.contains(tournamentStatus);
    }

    public String getId() {
        return id;
    }

    public boolean isCloseAccounts() {
        return closeAccounts;
    }

    /**
     * Find a status by its persisted ID.
     *
     * @param id the ID.
     * @return the status.
     * @throws IllegalArgumentException if the ID is invalid.
     */
    public static TournamentStatus getById(final String id) {
        notBlank(id, "ID may not be blank");

        for (final TournamentStatus status : values()) {
            if (status.getId().equalsIgnoreCase(id)) {
                return status;
            }
        }

        throw new IllegalArgumentException("ID is invalid: " + id);
    }
}
