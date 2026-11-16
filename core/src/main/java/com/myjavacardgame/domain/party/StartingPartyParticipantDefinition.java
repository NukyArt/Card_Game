package com.myjavacardgame.domain.party;

import com.myjavacardgame.domain.cards.CardDefinition;
import com.myjavacardgame.domain.cards.CardRole;
import java.util.List;
import java.util.Objects;

/**
 * Immutable starting state assigned to one validated party participant.
 *
 * @param id stable, nonblank participant identifier
 * @param type non-null participant control type
 * @param startingGold nonnegative starting gold amount
 * @param startingBaseCards nonempty immutable list of cards permitted to serve as bases
 */
public record StartingPartyParticipantDefinition(
        String id,
        ParticipantType type,
        int startingGold,
        List<CardDefinition> startingBaseCards) {
    /**
     * Creates a resolved starting-party participant definition.
     *
     * @param id stable, nonblank participant identifier
     * @param type non-null participant control type
     * @param startingGold nonnegative starting gold amount
     * @param startingBaseCards nonempty list of non-null card definitions allowed as bases
     * @throws NullPointerException if an argument or base card is null
     * @throws IllegalArgumentException if the identifier is blank, gold is negative, the base list
     *                                  is empty, or a card cannot be used as a base
     */
    public StartingPartyParticipantDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(startingBaseCards, "startingBaseCards");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Participant id must not be blank");
        }
        if (startingGold < 0) {
            throw new IllegalArgumentException("Starting gold must not be negative");
        }
        startingBaseCards = List.copyOf(startingBaseCards);
        if (startingBaseCards.isEmpty()) {
            throw new IllegalArgumentException("A participant must have at least one starting base");
        }
        for (CardDefinition card : startingBaseCards) {
            if (!card.allowedRoles().contains(CardRole.BASE)) {
                throw new IllegalArgumentException("Starting card must allow the base role: " + card.id());
            }
        }
    }
}
