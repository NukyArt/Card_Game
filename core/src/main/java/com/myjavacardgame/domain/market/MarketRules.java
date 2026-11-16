package com.myjavacardgame.domain.market;

import com.myjavacardgame.domain.board.BoardRules;
import java.util.Objects;

/** Immutable configuration for the market and placements purchased from it.
 *
 * @param visibleSlotCount number of face-up market positions
 * @param refreshGoldCost gold required for a non-free refresh
 * @param boardRules limits applied when a purchased card is placed on a board
 * @param salePriceDivisor positive divisor applied to each card's role price when selling
 */
public record MarketRules(int visibleSlotCount, int refreshGoldCost, BoardRules boardRules, int salePriceDivisor) {
    /**
     * Validates market configuration supplied by game data.
     *
     * @throws NullPointerException if {@code boardRules} is null
     * @throws IllegalArgumentException if a slot count or sale divisor is not positive, or refresh cost is negative
     */
    public MarketRules {
        if (visibleSlotCount <= 0 || refreshGoldCost < 0 || salePriceDivisor <= 0) {
            throw new IllegalArgumentException("Market slot count and sale divisor must be positive and refresh cost must not be negative");
        }
        Objects.requireNonNull(boardRules, "boardRules");
    }
}
