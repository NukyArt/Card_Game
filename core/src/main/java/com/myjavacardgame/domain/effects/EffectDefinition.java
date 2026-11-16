package com.myjavacardgame.domain.effects;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Immutable data describing an effect and its operations. */
public record EffectDefinition(String id, EffectTiming timing, EffectCondition condition,
        List<EffectOperation> operations, List<EffectChoiceRequirement> choices) {
    /** Validates and copies the effect definition. */
    public EffectDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(timing, "timing");
        Objects.requireNonNull(condition, "condition");
        operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        if (id.isBlank() || operations.isEmpty()) throw new IllegalArgumentException("Effect needs an id and operations");
        if (new HashSet<>(choices.stream().map(EffectChoiceRequirement::id).toList()).size() != choices.size()) throw new IllegalArgumentException("Duplicate choice requirements");
    }
}
