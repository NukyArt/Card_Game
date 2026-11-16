package com.myjavacardgame.domain.cards;

import java.util.Objects;

/** A physical card instance, independent of any role chosen when it is placed. */
public final class CardInstance {
    private final String instanceId;
    private final CardDefinition definition;

    private CardInstance(String instanceId, CardDefinition definition) {
        this.instanceId = instanceId;
        this.definition = definition;
    }

    /**
     * Resolves a card definition from the catalog and creates an instance.
     * The caller is responsible for assigning identifiers unique within the game.
     *
     * @param instanceId nonblank identifier of this physical card
     * @param cardDefinitionId identifier of a definition in the catalog
     * @param catalog catalog containing the definition
     * @return a new card instance
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if the instance identifier is blank or the definition is unknown
     */
    public static CardInstance fromCatalog(String instanceId, String cardDefinitionId, CardCatalog catalog) {
        Objects.requireNonNull(instanceId, "instanceId");
        Objects.requireNonNull(cardDefinitionId, "cardDefinitionId");
        Objects.requireNonNull(catalog, "catalog");
        if (instanceId.isBlank()) {
            throw new IllegalArgumentException("Card instance identifier must not be blank");
        }
        return new CardInstance(instanceId, catalog.requireCard(cardDefinitionId));
    }

    /**
     * Returns the identifier assigned to this physical card.
     *
     * @return the instance identifier
     */
    public String instanceId() {
        return instanceId;
    }

    /**
     * Returns the immutable definition resolved for this card.
     *
     * @return the card definition
     */
    public CardDefinition definition() {
        return definition;
    }
}
