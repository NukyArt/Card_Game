package com.myjavacardgame.domain.market;

import com.myjavacardgame.domain.cards.CardInstance;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable externally safe view of a market's visible positions and pile sizes.
 *
 * @param visibleCards ordered face-up positions; empty values are unfilled positions
 * @param drawPileCount number of cards still available to draw
 * @param discardPileCount number of cards in the shared discard
 */
public record MarketSnapshot(
        List<Optional<CardInstance>> visibleCards, int drawPileCount, int discardPileCount) {
    /** Defensively copies visible positions and verifies nonnegative pile sizes. */
    public MarketSnapshot {
        visibleCards = List.copyOf(Objects.requireNonNull(visibleCards, "visibleCards"));
        for (Optional<CardInstance> card : visibleCards) {
            Objects.requireNonNull(card, "visible card");
        }
        if (drawPileCount < 0 || discardPileCount < 0) {
            throw new IllegalArgumentException("Market pile counts must not be negative");
        }
    }
}
