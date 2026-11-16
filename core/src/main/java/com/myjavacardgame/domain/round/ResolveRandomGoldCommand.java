package com.myjavacardgame.domain.round;

import java.util.Objects;

/** Resolves one revealed random gold payout belonging to the active participant. */
public record ResolveRandomGoldCommand(String participantId, int goldToProject) implements GameSessionCommand {
    /** Validates the participant and nonnegative gold allocation. */
    public ResolveRandomGoldCommand {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.isBlank() || goldToProject < 0) throw new IllegalArgumentException("Invalid gold allocation");
    }
}
