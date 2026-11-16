package com.myjavacardgame.domain.round;

import java.util.Objects;

/** Requests transfer of one owned base and all of its upgrades to the current project stage. */
public record ContributeBoardPackageCommand(String participantId, int baseIndex) implements GameSessionCommand {
    /** Validates command input. */
    public ContributeBoardPackageCommand {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.isBlank() || baseIndex < 0) {
            throw new IllegalArgumentException("Participant id must be nonblank and base index nonnegative");
        }
    }
}
