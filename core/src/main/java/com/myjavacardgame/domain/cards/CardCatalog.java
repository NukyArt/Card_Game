package com.myjavacardgame.domain.cards;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable catalog of card definitions, indexed by their stable identifiers.
 *
 * @param definitionsById immutable mapping from each card identifier to its definition
 */
public record CardCatalog(Map<String, CardDefinition> definitionsById) {
    /**
     * Creates a catalog and verifies that every map key matches its card definition identifier.
     *
     * @param definitionsById non-null mapping of non-null identifiers to non-null card definitions
     * @throws NullPointerException if the map, an identifier, or a definition is null
     * @throws IllegalArgumentException if a map key does not match its definition identifier
     */
    public CardCatalog {
        Objects.requireNonNull(definitionsById, "definitionsById");
        definitionsById = Map.copyOf(definitionsById);
        for (Map.Entry<String, CardDefinition> entry : definitionsById.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().id())) {
                throw new IllegalArgumentException(
                        "Catalog key must match the card definition identifier: " + entry.getKey());
            }
        }
    }

    /**
     * Resolves a card definition by its stable identifier.
     *
     * @param cardId non-null card identifier
     * @return the catalogued card definition
     * @throws NullPointerException if {@code cardId} is null
     * @throws IllegalArgumentException if no card has the supplied identifier
     */
    public CardDefinition requireCard(String cardId) {
        Objects.requireNonNull(cardId, "cardId");
        CardDefinition definition = definitionsById.get(cardId);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown card identifier: " + cardId);
        }
        return definition;
    }
}
