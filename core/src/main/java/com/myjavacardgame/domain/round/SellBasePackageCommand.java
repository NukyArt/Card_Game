package com.myjavacardgame.domain.round;

import java.util.Objects;

/** Requests sale of one base and every upgrade attached to it as one market-turn transaction.
 *
 * @param participantId participant selling the package
 * @param baseIndex zero-based owned base index
 */
public record SellBasePackageCommand(String participantId, int baseIndex) implements GameSessionCommand {
    /** Validates command input without checking board ownership or sale limits. */
    public SellBasePackageCommand {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.isBlank() || baseIndex < 0) {
            throw new IllegalArgumentException("Participant id must be nonblank and base index nonnegative");
        }
    }
}
