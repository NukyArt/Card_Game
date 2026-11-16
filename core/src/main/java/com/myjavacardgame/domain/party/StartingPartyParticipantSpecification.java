package com.myjavacardgame.domain.party;

import java.util.List;
import java.util.Objects;

/**
 * Immutable external-data input for a starting-party participant before card references are resolved.
 *
 * @param id stable, nonblank participant identifier
 * @param type non-null participant control type
 * @param startingGold nonnegative starting gold amount
 * @param startingBaseCardIds nonempty immutable list of stable card identifiers
 */
public record StartingPartyParticipantSpecification(
        String id,
        ParticipantType type,
        int startingGold,
        List<String> startingBaseCardIds) {
    /**
     * Creates a starting-party participant specification.
     *
     * @param id stable, nonblank participant identifier
     * @param type non-null participant control type
     * @param startingGold nonnegative starting gold amount
     * @param startingBaseCardIds nonempty list of nonblank card identifiers
     * @throws NullPointerException if an argument or card identifier is null
     * @throws IllegalArgumentException if the identifier is blank, gold is negative, the base list
     *                                  is empty, or a base card identifier is blank
     */
    public StartingPartyParticipantSpecification {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(startingBaseCardIds, "startingBaseCardIds");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Participant id must not be blank");
        }
        if (startingGold < 0) {
            throw new IllegalArgumentException("Starting gold must not be negative");
        }
        startingBaseCardIds = List.copyOf(startingBaseCardIds);
        if (startingBaseCardIds.isEmpty()) {
            throw new IllegalArgumentException("A participant must have at least one starting base reference");
        }
        for (String cardId : startingBaseCardIds) {
            if (cardId.isBlank()) {
                throw new IllegalArgumentException("Starting base card identifiers must not be blank");
            }
        }
    }
}
