package com.myjavacardgame.domain.round;

/**
 * Identifies the current phase of a round or the terminal match-result phase.
 */
public enum RoundPhase {
    /** Income is applied before market activity begins. */
    INCOME,

    /** Market activity occurs after income and before production. */
    MARKET,

    /** Production occurs after the market phase. */
    PRODUCTION,

    /** Deposit resolution occurs after production. */
    DEPOSIT,

    /** The match has ended and no further phase commands are valid. */
    RESULT
}
