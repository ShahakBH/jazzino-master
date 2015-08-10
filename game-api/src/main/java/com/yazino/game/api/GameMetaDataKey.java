package com.yazino.game.api;

/**
 * Types of meta-data available to games.
 */
public enum GameMetaDataKey {

    /**
     * The message published when a player ranks in a tournament.
     */
    TOURNAMENT_RANKING_MESSAGE,

    /**
     * The summary message received when a player ranks in a tournament.
     */
    TOURNAMENT_SUMMARY_MESSAGE,

    /**
     * The prefix used by the tournament position achievements.
     * <p/>
     * If missing then the game type is used.
     */
    TOURNAMENT_ACHIEVEMENT_PREFIX

}
