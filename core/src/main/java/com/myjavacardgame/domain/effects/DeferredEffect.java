package com.myjavacardgame.domain.effects;

import java.util.List;
import java.util.Objects;

/** An effect retained for a future lifecycle rule. */
public record DeferredEffect(EffectDefinition definition, String sourceId, List<EffectChoice> choices) {
    /** Validates and copies the effect data. */
    public DeferredEffect {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(sourceId, "sourceId");
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        if (sourceId.isBlank() || definition.timing() != EffectTiming.DEFERRED) throw new IllegalArgumentException("Invalid deferred effect");
    }
}
