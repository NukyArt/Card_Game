package com.myjavacardgame.domain.cards;

import java.util.Objects;

/** A card instance with a role fixed for one placement.
 *
 * @param instance the physical card
 * @param role a role permitted by the card definition
 */
public record PlacedCard(CardInstance instance, CardRole role) {
    /**
     * Creates a placement with a role allowed by the card definition.
     *
     * @param instance the non-null physical card
     * @param role the non-null role selected for this placement
     * @throws NullPointerException if either argument is null
     * @throws IllegalArgumentException if the definition does not permit the role
     */
    public PlacedCard {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(role, "role");
        if (!instance.definition().allowedRoles().contains(role)) {
            throw new IllegalArgumentException("Card definition does not allow role: " + role);
        }
    }

    /**
     * Returns the parameters for the role fixed by this placement.
     *
     * @return the selected role's parameters
     */
    public CardRoleParameters roleParameters() {
        return instance.definition().roleParameters().get(role);
    }
}
