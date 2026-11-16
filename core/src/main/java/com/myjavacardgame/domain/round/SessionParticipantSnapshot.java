package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.party.ParticipantType;
import java.util.Objects;

/**
 * Immutable participant data exposed in a {@link GameSessionSnapshot}.
 *
 * @param id stable participant identifier
 * @param type participant control type
 */
public record SessionParticipantSnapshot(String id, ParticipantType type) {
    /**
     * Creates an immutable participant snapshot.
     *
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if {@code id} is blank
     */
    public SessionParticipantSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Participant id must not be blank");
        }
    }
}
