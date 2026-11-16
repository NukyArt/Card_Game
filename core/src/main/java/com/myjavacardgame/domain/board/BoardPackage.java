package com.myjavacardgame.domain.board;

import com.myjavacardgame.domain.cards.PlacedCard;
import java.util.List;
import java.util.OptionalInt;

/** An extracted base and its complete set of upgrades and activation history. */
public final class BoardPackage {
    private final BoardSlot slot;

    BoardPackage(BoardSlot slot) {
        this.slot = slot;
    }

    /** @return the base in its fixed role */
    public PlacedCard base() {
        return slot.base();
    }

    /** @return an immutable list of upgrades in placement order */
    public List<PlacedCard> upgrades() {
        return slot.upgrades();
    }

    /** @return the number of physical cards in the package */
    public int cardCount() {
        return 1 + slot.upgrades().size();
    }

    /** @return the last round of activation, or empty if never activated */
    public OptionalInt lastActivatedRound() {
        return slot.lastActivatedRound();
    }
}
