package com.myjavacardgame.domain.board;

import com.myjavacardgame.domain.cards.PlacedCard;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/** A base, its ordered upgrades, and its activation history. */
public final class BoardSlot {
    private final PlacedCard base;
    private final List<PlacedCard> upgrades;
    private final OptionalInt lastActivatedRound;

    BoardSlot(PlacedCard base, List<PlacedCard> upgrades) {
        this(base, upgrades, OptionalInt.empty());
    }

    BoardSlot(PlacedCard base, List<PlacedCard> upgrades, OptionalInt lastActivatedRound) {
        this.base = Objects.requireNonNull(base, "base");
        this.upgrades = List.copyOf(upgrades);
        this.lastActivatedRound = Objects.requireNonNull(lastActivatedRound, "lastActivatedRound");
        if (lastActivatedRound.isPresent() && lastActivatedRound.getAsInt() < 1) {
            throw new IllegalArgumentException("Round number must be positive");
        }
    }

    /**
     * Returns the placed base.
     *
     * @return the base card
     */
    public PlacedCard base() {
        return base;
    }

    /**
     * Returns the upgrades in placement order.
     *
     * @return an immutable list of placed upgrades
     */
    public List<PlacedCard> upgrades() {
        return upgrades;
    }

    /**
     * Returns the most recent round in which this base was activated.
     *
     * @return the positive round number, or empty if the base has not been activated
     */
    public OptionalInt lastActivatedRound() {
        return lastActivatedRound;
    }
}
