package com.myjavacardgame.domain.board;

import com.myjavacardgame.domain.cards.PlacedCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Immutable board operations for extracting cards and tracking base activation. */
public final class BoardOperations {
    private BoardOperations() {
    }

    /**
     * Removes a base together with all attached upgrades.
     *
     * @param board the current board
     * @param baseIndex zero-based index of the base
     * @return the new board and complete package, or the original board and an empty package if invalid
     * @throws NullPointerException if the board is null
     */
    public static BaseExtractionResult extractBase(BoardSnapshot board, int baseIndex) {
        Objects.requireNonNull(board, "board");
        if (!validBaseIndex(board, baseIndex)) {
            return new BaseExtractionResult(board, Optional.empty());
        }
        List<BoardSlot> slots = new ArrayList<>(board.slots());
        BoardSlot removed = slots.remove(baseIndex);
        return new BaseExtractionResult(new BoardSnapshot(slots), Optional.of(new BoardPackage(removed)));
    }

    /**
     * Removes one upgrade while retaining the base, other upgrades and activation history.
     *
     * @param board the current board
     * @param baseIndex zero-based index of the base
     * @param upgradeIndex zero-based index of its upgrade
     * @return the new board and removed upgrade, or the original board and an empty upgrade if invalid
     * @throws NullPointerException if the board is null
     */
    public static UpgradeExtractionResult extractUpgrade(BoardSnapshot board, int baseIndex, int upgradeIndex) {
        Objects.requireNonNull(board, "board");
        if (!validBaseIndex(board, baseIndex)) {
            return new UpgradeExtractionResult(board, Optional.empty());
        }
        BoardSlot slot = board.slots().get(baseIndex);
        if (upgradeIndex < 0 || upgradeIndex >= slot.upgrades().size()) {
            return new UpgradeExtractionResult(board, Optional.empty());
        }
        List<PlacedCard> upgrades = new ArrayList<>(slot.upgrades());
        PlacedCard removed = upgrades.remove(upgradeIndex);
        List<BoardSlot> slots = new ArrayList<>(board.slots());
        slots.set(baseIndex, new BoardSlot(slot.base(), upgrades, slot.lastActivatedRound()));
        return new UpgradeExtractionResult(new BoardSnapshot(slots), Optional.of(removed));
    }

    /**
     * Records an activation if the base has not already activated in this or a later round.
     * No production or card effects are applied.
     *
     * @param board the current board
     * @param baseIndex zero-based index of the base
     * @param roundNumber positive current round number
     * @return the new board and success, or the original board and failure for an invalid index or repeated/stale round
     * @throws NullPointerException if the board is null
     * @throws IllegalArgumentException if the round number is not positive
     */
    public static BoardActivationResult tryActivateBase(BoardSnapshot board, int baseIndex, int roundNumber) {
        Objects.requireNonNull(board, "board");
        if (roundNumber < 1) {
            throw new IllegalArgumentException("Round number must be positive");
        }
        if (!validBaseIndex(board, baseIndex)) {
            return new BoardActivationResult(board, false);
        }
        BoardSlot slot = board.slots().get(baseIndex);
        if (slot.lastActivatedRound().isPresent() && slot.lastActivatedRound().getAsInt() >= roundNumber) {
            return new BoardActivationResult(board, false);
        }
        List<BoardSlot> slots = new ArrayList<>(board.slots());
        slots.set(baseIndex, new BoardSlot(slot.base(), slot.upgrades(), OptionalInt.of(roundNumber)));
        return new BoardActivationResult(new BoardSnapshot(slots), true);
    }

    private static boolean validBaseIndex(BoardSnapshot board, int baseIndex) {
        return baseIndex >= 0 && baseIndex < board.slots().size();
    }
}
