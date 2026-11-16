package com.myjavacardgame.domain.board;

import java.util.List;

/** Immutable snapshot of the ordered base slots on a player's board. */
public final class BoardSnapshot {
    private final List<BoardSlot> slots;

    BoardSnapshot(List<BoardSlot> slots) {
        this.slots = List.copyOf(slots);
    }

    /**
     * Creates an empty board snapshot.
     *
     * @return a snapshot with no bases
     */
    public static BoardSnapshot empty() {
        return new BoardSnapshot(List.of());
    }

    /**
     * Returns the base slots in placement order.
     *
     * @return an immutable list of immutable slots
     */
    public List<BoardSlot> slots() {
        return slots;
    }
}
