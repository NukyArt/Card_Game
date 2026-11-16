package com.myjavacardgame.domain.effects;

import java.util.Objects;
import java.util.Set;

/** Data-defined options and whether a choice is required. */
public record EffectChoiceRequirement(String id, Set<String> options, boolean required) {
    /** Validates and copies the options. */
    public EffectChoiceRequirement {
        Objects.requireNonNull(id, "id");
        options = Set.copyOf(Objects.requireNonNull(options, "options"));
        if (id.isBlank() || options.isEmpty() || options.stream().anyMatch(String::isBlank)) throw new IllegalArgumentException("Invalid choice requirement");
    }
}
