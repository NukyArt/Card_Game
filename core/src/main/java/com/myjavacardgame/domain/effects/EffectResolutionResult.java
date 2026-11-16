package com.myjavacardgame.domain.effects;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Candidate effect state and ordered operations or a rejection. */
public record EffectResolutionResult(boolean accepted, Optional<String> rejectionReason,
        EffectResolutionState state, List<CreditGoldOperation> operations) {
    /** Validates and copies the result. */
    public EffectResolutionResult {
        Objects.requireNonNull(rejectionReason, "rejectionReason");
        Objects.requireNonNull(state, "state");
        operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
        if (accepted == rejectionReason.isPresent() || (!accepted && !operations.isEmpty())) throw new IllegalArgumentException("Invalid effect result");
    }
}
