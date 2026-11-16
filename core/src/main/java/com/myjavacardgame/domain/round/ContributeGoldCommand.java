package com.myjavacardgame.domain.round;

import java.util.Objects;

/** Requests that one participant contribute personal gold to the current project stage. */
public record ContributeGoldCommand(String participantId, int amount) implements GameSessionCommand {
    /** Validates command input without interpreting the participant balance or stage demand. */
    public ContributeGoldCommand {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.isBlank() || amount < 0) {
            throw new IllegalArgumentException("Participant id must be nonblank and gold must be nonnegative");
        }
    }
}
