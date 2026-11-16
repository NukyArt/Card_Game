package com.myjavacardgame.domain.cards;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Data-defined production for one card role. */
public record CardProduction(Map<ResourceType, Integer> amounts, List<ProductionEffect> effects) {
    /** Validates and defensively copies production data. */
    public CardProduction {
        amounts = Map.copyOf(Objects.requireNonNull(amounts, "amounts"));
        effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
        if (amounts.values().stream().anyMatch(amount -> amount < 0)) {
            throw new IllegalArgumentException("Production amounts must not be negative");
        }
    }

    /** Returns production with no resource payouts or effects. */
    public static CardProduction empty() {
        return new CardProduction(Map.of(), List.of());
    }
}
