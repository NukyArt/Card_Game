package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.cards.CardRole;
import java.util.Objects;

/**
 * Immutable effective-cost quotation for one market purchase currently accepted by a {@link GameSession}.
 *
 * @param marketSlot zero-based visible market position
 * @param role selected permitted role for the purchased card
 * @param baseIndex target base for an upgrade, or {@code -1} for a base
 * @param effectiveGoldCost final current gold cost after applicable domain effects
 */
public record MarketPurchaseQuote(int marketSlot, CardRole role, int baseIndex, int effectiveGoldCost) {
    /** Validates the command target shape and nonnegative effective cost. */
    public MarketPurchaseQuote {
        Objects.requireNonNull(role, "role");
        if (marketSlot < 0 || effectiveGoldCost < 0
                || (role == CardRole.BASE && baseIndex != -1)
                || (role == CardRole.UPGRADE && baseIndex < 0)) {
            throw new IllegalArgumentException("Market purchase quote values are invalid");
        }
    }
}
