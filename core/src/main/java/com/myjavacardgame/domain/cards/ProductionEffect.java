package com.myjavacardgame.domain.cards;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.List;
import java.util.Objects;

/** Data-defined additional production effect and its configurable values. */
public record ProductionEffect(Kind kind, int amount, int cost, List<ResourceType> resources) {
    /** Supported activation effect categories. */
    public enum Kind { FIXED, CHOICE, REPLACE_BASE, PER_OTHER_BASE, EXCHANGE, RANDOM, TRANSFER, DEFERRED, DEFERRED_PURCHASE,
        NEXT_SALE_BONUS, NEXT_TYPE_DISCOUNT, NEXT_FREE_REFRESH, MATCHING_PURCHASE_PAYOUT }

    /** Validates and copies the effect parameters. */
    public ProductionEffect {
        Objects.requireNonNull(kind, "kind");
        resources = List.copyOf(Objects.requireNonNull(resources, "resources"));
        if (amount < 0 || cost < 0) throw new IllegalArgumentException("Effect values must not be negative");
    }
}
