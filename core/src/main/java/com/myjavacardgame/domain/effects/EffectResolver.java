package com.myjavacardgame.domain.effects;

/** Computes candidate changes without mutating the session. */
public interface EffectResolver {
    /** Resolves a request against the current effect state. */
    EffectResolutionResult resolve(EffectResolutionState state, EffectResolutionRequest request);
    /** Applies active effects in insertion order. */
    EffectResolutionResult applyPersistent(EffectResolutionState state);
}
