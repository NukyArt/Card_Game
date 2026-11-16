package com.myjavacardgame.domain.cards;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.Map;
import java.util.Objects;

/** Parameters specified independently for one role of a card.
 *
 * @param prices resource amounts required for this role; omitted resources have no listed price
 * @param production structured production data for this role
 */
public record CardRoleParameters(Map<ResourceType, Integer> prices, CardProduction production) {
    /** Creates role parameters without configured production. */
    public CardRoleParameters(Map<ResourceType, Integer> prices) {
        this(prices, CardProduction.empty());
    }
    /**
     * Creates role parameters with an immutable copy of the prices.
     *
     * @param prices non-null resource prices with non-null, nonnegative amounts
     * @throws NullPointerException if the map, a resource, or an amount is null
     * @throws IllegalArgumentException if any amount is negative
     */
    public CardRoleParameters {
        Objects.requireNonNull(prices, "prices");
        Objects.requireNonNull(production, "production");
        prices = Map.copyOf(prices);
        for (int amount : prices.values()) {
            if (amount < 0) {
                throw new IllegalArgumentException("Prices must not be negative");
            }
        }
    }
}
