package com.myjavacardgame.domain.round;

import java.util.Objects;

/** Requests sale of one upgrade as one market-turn transaction.
 *
 * @param participantId participant selling the upgrade
 * @param baseIndex zero-based owned base index
 * @param upgradeIndex zero-based upgrade index under that base
 */
public record SellUpgradeCommand(String participantId, int baseIndex, int upgradeIndex)
        implements GameSessionCommand {
    /** Validates command input without checking board ownership or sale limits. */
    public SellUpgradeCommand {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.isBlank() || baseIndex < 0 || upgradeIndex < 0) {
            throw new IllegalArgumentException("Participant id and board indices are invalid");
        }
    }
}
