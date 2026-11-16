package com.myjavacardgame.domain.effects;

import java.util.List;
import java.util.Objects;

/** A persistent effect and its original explicit choices. */
public record ActivePersistentEffect(EffectDefinition definition, String sourceId, List<EffectChoice> choices) {
    /** Validates and copies the effect data. */
    public ActivePersistentEffect {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(sourceId, "sourceId");
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        if (sourceId.isBlank() || definition.timing() != EffectTiming.PERMANENT) throw new IllegalArgumentException("Invalid persistent effect");
    }
}
