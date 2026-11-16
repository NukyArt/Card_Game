package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.Objects;

/** Requests that a produced non-gold resource be paid into the current project stage. */
public record ContributeResourceCommand(String participantId, ResourceType resource, int amount)
        implements GameSessionCommand {
    /** Validates command input. Gold must use {@link ContributeGoldCommand}. */
    public ContributeResourceCommand {
        Objects.requireNonNull(participantId, "participantId");
        Objects.requireNonNull(resource, "resource");
        if (participantId.isBlank() || amount < 0 || resource == ResourceType.GOLD) {
            throw new IllegalArgumentException("Resource contribution must identify a participant, be nonnegative, and not be gold");
        }
    }
}
