package com.myjavacardgame.domain.effects;

import com.myjavacardgame.domain.random.RandomSource;
import java.util.List;
import java.util.Objects;

/** Explicit inputs for resolving an effect. */
public record EffectResolutionRequest(String sourceId, EffectDefinition definition,
        List<EffectChoice> choices, RandomSource randomSource) {
    /** Validates and copies the request. */
    public EffectResolutionRequest {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(definition, "definition");
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        Objects.requireNonNull(randomSource, "randomSource");
        if (sourceId.isBlank()) throw new IllegalArgumentException("Effect source must not be blank");
    }
}
