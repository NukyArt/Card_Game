package com.myjavacardgame.domain.events;

import java.util.Objects;

/** Notification that an effect request was committed. */
public record EffectResolvedEvent(String effectId, String sourceId) implements DomainEvent {
    /** Validates event identifiers. */
    public EffectResolvedEvent {
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(sourceId, "sourceId");
        if (effectId.isBlank() || sourceId.isBlank()) throw new IllegalArgumentException("Invalid effect event");
    }
}
