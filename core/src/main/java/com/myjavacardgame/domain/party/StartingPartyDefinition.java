package com.myjavacardgame.domain.party;

import com.myjavacardgame.domain.cards.CardCatalog;
import com.myjavacardgame.domain.cards.CardDefinition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable starting-party definition with catalog-validated base-card references.
 *
 * <p>For example, an application can pass one {@link ParticipantType#HUMAN} specification and
 * three {@link ParticipantType#BOT} specifications, each with its configured starting gold and
 * four starting-base identifiers, to {@link #fromSpecifications(List, CardCatalog)}.</p>
 */
public final class StartingPartyDefinition {
    private final List<StartingPartyParticipantDefinition> participants;

    private StartingPartyDefinition(List<StartingPartyParticipantDefinition> participants) {
        this.participants = List.copyOf(participants);
        if (this.participants.isEmpty()) {
            throw new IllegalArgumentException("A starting party must contain at least one participant");
        }
        Set<String> participantIds = new HashSet<>();
        for (StartingPartyParticipantDefinition participant : this.participants) {
            if (!participantIds.add(participant.id())) {
                throw new IllegalArgumentException(
                        "Starting-party participant identifiers must be unique: " + participant.id());
            }
        }
    }

    /**
     * Resolves base-card identifiers through a catalog and creates a complete starting-party definition.
     *
     * <p>All specifications are resolved before this method returns. Therefore an unknown card or a
     * card that cannot serve as a base rejects the request without returning a partial party.</p>
     *
     * @param specifications nonempty external-data specifications for party participants
     * @param catalog non-null catalog used to validate each base-card identifier
     * @return an immutable party definition containing resolved card definitions
     * @throws NullPointerException if an argument, specification, or referenced identifier is null
     * @throws IllegalArgumentException if any specification is invalid, a participant identifier is
     *                                  duplicated, a card is unknown, or a referenced card is not a base
     */
    public static StartingPartyDefinition fromSpecifications(
            List<StartingPartyParticipantSpecification> specifications, CardCatalog catalog) {
        Objects.requireNonNull(specifications, "specifications");
        Objects.requireNonNull(catalog, "catalog");

        List<StartingPartyParticipantDefinition> participants = new ArrayList<>(specifications.size());
        for (StartingPartyParticipantSpecification specification : specifications) {
            Objects.requireNonNull(specification, "specification");
            List<CardDefinition> startingBases = new ArrayList<>(specification.startingBaseCardIds().size());
            for (String cardId : specification.startingBaseCardIds()) {
                startingBases.add(catalog.requireCard(cardId));
            }
            participants.add(new StartingPartyParticipantDefinition(
                    specification.id(),
                    specification.type(),
                    specification.startingGold(),
                    startingBases));
        }
        return new StartingPartyDefinition(participants);
    }

    /**
     * Returns the party participants in their configured order.
     *
     * @return an immutable list of resolved participant definitions
     */
    public List<StartingPartyParticipantDefinition> participants() {
        return participants;
    }
}
