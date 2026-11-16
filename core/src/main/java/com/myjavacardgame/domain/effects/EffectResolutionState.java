package com.myjavacardgame.domain.effects;

import java.util.List;
import java.util.Objects;

/** Immutable active and deferred effects in insertion order. */
public record EffectResolutionState(List<ActivePersistentEffect> active, List<DeferredEffect> deferred) {
    /** Copies effect collections. */
    public EffectResolutionState {
        active = List.copyOf(Objects.requireNonNull(active, "active"));
        deferred = List.copyOf(Objects.requireNonNull(deferred, "deferred"));
    }
    /** Returns an empty state. */
    public static EffectResolutionState empty() { return new EffectResolutionState(List.of(), List.of()); }
}
