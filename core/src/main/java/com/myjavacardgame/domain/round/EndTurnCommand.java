package com.myjavacardgame.domain.round;

import java.util.Objects;

/**
 * Requests completion of the current participant's market or production turn.
 *
 * @param participantId identifier of the participant ending a turn
 */
public record EndTurnCommand(String participantId) implements GameSessionCommand {
    /**
     * Validates the participant identifier.
     *
     * @throws NullPointerException if {@code participantId} is null
     * @throws IllegalArgumentException if {@code participantId} is blank
     */
    public EndTurnCommand {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.isBlank()) {
            throw new IllegalArgumentException("Participant id must not be blank");
        }
    }
}
