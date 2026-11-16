package com.myjavacardgame.domain.events;

import java.util.Objects;

/** Notification that gold was credited to a participant. */
public record GoldCreditedEvent(String participantId, int amount) implements DomainEvent {
    /** Validates the event. */
    public GoldCreditedEvent {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.isBlank() || amount <= 0) throw new IllegalArgumentException("Invalid credit event");
    }
}
