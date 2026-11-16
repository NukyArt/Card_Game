package com.myjavacardgame.domain.round;

import java.util.Objects;

/** Requests replacement of every face-up market card.
 *
 * @param participantId participant using its market-turn refresh
 */
public record RefreshMarketCommand(String participantId) implements GameSessionCommand {
    /** Validates the participant identifier. */
    public RefreshMarketCommand {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.isBlank()) {
            throw new IllegalArgumentException("Participant id must not be blank");
        }
    }
}
