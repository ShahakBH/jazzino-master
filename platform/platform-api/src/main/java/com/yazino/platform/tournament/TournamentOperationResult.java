package com.yazino.platform.tournament;

/**
 * The available tournament operation results.
 */
public enum TournamentOperationResult {

    SUCCESS,
    MAX_PLAYERS_EXCEEDED,
    INVALID_SIGNUP_STATE,
    BEFORE_SIGNUP_TIME,
    AFTER_SIGNUP_TIME,
    PLAYER_ALREADY_REGISTERED,
    PLAYER_NOT_REGISTERED,
    TRANSFER_FAILED,
    UNKNOWN,
    NO_RESPONSE_RETURNED
}
