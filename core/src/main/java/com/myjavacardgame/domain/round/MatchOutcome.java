package com.myjavacardgame.domain.round;

/**
 * Terminal or ongoing outcome of a game session.
 */
public enum MatchOutcome {
    /** The project is still incomplete and the session accepts game commands. */
    IN_PROGRESS,

    /** The project was completed during the market or production phase. */
    VICTORY,

    /** The round limit was reached before the project was completed. */
    DEFEAT
}
