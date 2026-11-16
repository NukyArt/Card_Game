package com.myjavacardgame.domain.effects;

import java.util.Objects;

/** A participant's explicit option for a choice requirement. */
public record EffectChoice(String requirementId, String option) {
    /** Validates the choice. */
    public EffectChoice {
        Objects.requireNonNull(requirementId, "requirementId");
        Objects.requireNonNull(option, "option");
        if (requirementId.isBlank() || option.isBlank()) throw new IllegalArgumentException("Choice must not be blank");
    }
}
