package com.myjavacardgame.domain.round;

/**
 * A command that can advance the round phase machine.
 *
 * <p>The command only expresses a requested phase transition. It does not model player actions,
 * match outcome rules, participants, or a round limit.</p>
 */
public enum PhaseCommand {
    /** Requests transition from income to market. */
    COMPLETE_INCOME,

    /** Requests transition from market to production. */
    COMPLETE_MARKET,

    /** Requests transition from production to deposit. */
    COMPLETE_PRODUCTION,

    /** Requests transition from deposit to the next round's income phase. */
    COMPLETE_DEPOSIT,

    /** Requests transition to the terminal match-result phase when the current phase permits it. */
    END_MATCH
}
