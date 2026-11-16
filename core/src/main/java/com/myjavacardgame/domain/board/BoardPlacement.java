package com.myjavacardgame.domain.board;

import com.myjavacardgame.domain.cards.CardInstance;
import com.myjavacardgame.domain.cards.CardRole;
import com.myjavacardgame.domain.cards.PlacedCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Creates new board snapshots after validating card placement. */
public final class BoardPlacement {
    private BoardPlacement() {
    }

    /**
     * Adds a card in the base role after the existing bases.
     *
     * @param board current board snapshot
     * @param card physical card to place
     * @param rules board limits supplied by the game rules
     * @return a new board snapshot containing the base
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the card is already placed, cannot be a base, or the board is full
     */
    public static BoardSnapshot placeBase(BoardSnapshot board, CardInstance card, BoardRules rules) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(card, "card");
        Objects.requireNonNull(rules, "rules");
        requireNotPlaced(board, card);
        if (board.slots().size() >= rules.maxBases()) {
            throw new IllegalArgumentException("Board has no free base slot");
        }
        PlacedCard base = new PlacedCard(card, CardRole.BASE);
        List<BoardSlot> slots = new ArrayList<>(board.slots());
        slots.add(new BoardSlot(base, List.of()));
        return new BoardSnapshot(slots);
    }

    /**
     * Adds a card in the upgrade role under a compatible base.
     *
     * @param board current board snapshot
     * @param card physical card to place
     * @param baseIndex zero-based index of the target base
     * @param rules board limits supplied by the game rules
     * @return a new board snapshot containing the upgrade
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the card is already placed, the index is invalid, the role or type
     *                                  is incompatible, or the base has no free upgrade slot
     */
    public static BoardSnapshot placeUpgrade(
            BoardSnapshot board, CardInstance card, int baseIndex, BoardRules rules) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(card, "card");
        Objects.requireNonNull(rules, "rules");
        requireNotPlaced(board, card);
        if (baseIndex < 0 || baseIndex >= board.slots().size()) {
            throw new IllegalArgumentException("Invalid base index: " + baseIndex);
        }
        BoardSlot slot = board.slots().get(baseIndex);
        if (slot.upgrades().size() >= rules.maxUpgradesPerBase()) {
            throw new IllegalArgumentException("Base has no free upgrade slot");
        }
        if (!slot.base().instance().definition().type().equals(card.definition().type())) {
            throw new IllegalArgumentException("Upgrade type does not match base type");
        }
        PlacedCard upgrade = new PlacedCard(card, CardRole.UPGRADE);
        List<PlacedCard> upgrades = new ArrayList<>(slot.upgrades());
        upgrades.add(upgrade);
        List<BoardSlot> slots = new ArrayList<>(board.slots());
        slots.set(baseIndex, new BoardSlot(slot.base(), upgrades, slot.lastActivatedRound()));
        return new BoardSnapshot(slots);
    }

    /**
     * Places a previously extracted base and all its upgrades, retaining activation history.
     *
     * @param board destination board
     * @param cardPackage package returned by a base extraction
     * @param rules destination board limits
     * @return a new board snapshot containing the package
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if limits are exceeded or a card is already on the board
     */
    public static BoardSnapshot placePackage(BoardSnapshot board, BoardPackage cardPackage, BoardRules rules) {
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(cardPackage, "cardPackage");
        Objects.requireNonNull(rules, "rules");
        if (board.slots().size() >= rules.maxBases()) {
            throw new IllegalArgumentException("Board has no free base slot");
        }
        if (cardPackage.upgrades().size() > rules.maxUpgradesPerBase()) {
            throw new IllegalArgumentException("Package exceeds the upgrade limit");
        }
        requireNotPlaced(board, cardPackage.base().instance());
        for (PlacedCard upgrade : cardPackage.upgrades()) {
            requireNotPlaced(board, upgrade.instance());
        }
        List<BoardSlot> slots = new ArrayList<>(board.slots());
        slots.add(new BoardSlot(cardPackage.base(), cardPackage.upgrades(), cardPackage.lastActivatedRound()));
        return new BoardSnapshot(slots);
    }

    private static void requireNotPlaced(BoardSnapshot board, CardInstance card) {
        String instanceId = card.instanceId();
        for (BoardSlot slot : board.slots()) {
            if (slot.base().instance().instanceId().equals(instanceId)) {
                throw new IllegalArgumentException("Card instance is already on the board: " + instanceId);
            }
            for (PlacedCard upgrade : slot.upgrades()) {
                if (upgrade.instance().instanceId().equals(instanceId)) {
                    throw new IllegalArgumentException("Card instance is already on the board: " + instanceId);
                }
            }
        }
    }
}
