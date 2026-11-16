package com.myjavacardgame.domain.market;

import com.myjavacardgame.domain.cards.CardInstance;
import com.myjavacardgame.domain.random.RandomSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Mutable clean-domain market holding face-up cards, a draw pile, and a shared discard pile.
 * This class is not thread-safe; one {@code GameSession} owns and mutates it on its command thread.
 */
public final class MarketDeck {
    private final MarketRules rules;
    private final RandomSource randomSource;
    private final List<CardInstance> drawPile;
    private final List<CardInstance> discardPile;
    private final List<CardInstance> visibleCards;

    /**
     * Creates a market and immediately fills each visible position from the supplied deck.
     *
     * @param rules immutable market and board-placement configuration
     * @param marketCards physical cards initially in the draw pile
     * @param randomSource injected source used only when recycling the discard pile
     */
    public MarketDeck(MarketRules rules, List<CardInstance> marketCards, RandomSource randomSource) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.randomSource = Objects.requireNonNull(randomSource, "randomSource");
        Objects.requireNonNull(marketCards, "marketCards");
        validateUniqueInstances(marketCards);
        drawPile = new ArrayList<>(marketCards);
        discardPile = new ArrayList<>();
        visibleCards = new ArrayList<>(rules.visibleSlotCount());
        for (int index = 0; index < rules.visibleSlotCount(); index++) {
            visibleCards.add(drawNext().orElse(null));
        }
    }

    private MarketDeck(MarketDeck source) {
        rules = source.rules;
        randomSource = source.randomSource;
        drawPile = new ArrayList<>(source.drawPile);
        discardPile = new ArrayList<>(source.discardPile);
        visibleCards = new ArrayList<>(source.visibleCards);
    }

    /**
     * Returns an independent mutable copy for a newly created game session.
     *
     * @return copy with the same card order and visible positions
     */
    public MarketDeck copy() {
        return new MarketDeck(this);
    }

    /** @return immutable market configuration */
    public MarketRules rules() {
        return rules;
    }

    /** Returns the card at a face-up position without changing market state. */
    public Optional<CardInstance> cardAt(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= visibleCards.size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(visibleCards.get(slotIndex));
    }

    /** @return whether either the draw pile or shared discard has a card before a refresh */
    public boolean canRefresh() {
        return !drawPile.isEmpty() || !discardPile.isEmpty();
    }

    /**
     * Removes the selected face-up card and immediately attempts to refill that position.
     *
     * @param slotIndex zero-based visible position
     * @return removed card, or empty for an invalid or empty position
     */
    public Optional<CardInstance> takeCard(int slotIndex) {
        Optional<CardInstance> card = cardAt(slotIndex);
        if (card.isEmpty()) {
            return Optional.empty();
        }
        visibleCards.set(slotIndex, drawNext().orElse(null));
        return card;
    }

    /**
     * Discards every visible card and attempts to refill every position.
     * Callers must check {@link #canRefresh()} before calling this method.
     */
    public void refresh() {
        for (int index = 0; index < visibleCards.size(); index++) {
            CardInstance card = visibleCards.get(index);
            if (card != null) {
                discardPile.add(card);
            }
            visibleCards.set(index, null);
        }
        for (int index = 0; index < visibleCards.size(); index++) {
            visibleCards.set(index, drawNext().orElse(null));
        }
    }

    /** Adds cards removed from boards or project contributions to the shared discard pile. */
    public void discard(List<CardInstance> cards) {
        Objects.requireNonNull(cards, "cards");
        for (CardInstance card : cards) {
            discardPile.add(Objects.requireNonNull(card, "card"));
        }
    }

    /** @return immutable state suitable for a session snapshot */
    public MarketSnapshot snapshot() {
        List<Optional<CardInstance>> slots = new ArrayList<>(visibleCards.size());
        for (CardInstance card : visibleCards) {
            slots.add(Optional.ofNullable(card));
        }
        return new MarketSnapshot(slots, drawPile.size(), discardPile.size());
    }

    private Optional<CardInstance> drawNext() {
        if (drawPile.isEmpty() && !discardPile.isEmpty()) {
            recycleDiscard();
        }
        return drawPile.isEmpty() ? Optional.empty() : Optional.of(drawPile.remove(drawPile.size() - 1));
    }

    private void recycleDiscard() {
        drawPile.addAll(discardPile);
        discardPile.clear();
        for (int index = drawPile.size() - 1; index > 0; index--) {
            int swapIndex = randomSource.nextInt(index + 1);
            CardInstance card = drawPile.get(index);
            drawPile.set(index, drawPile.get(swapIndex));
            drawPile.set(swapIndex, card);
        }
    }

    private static void validateUniqueInstances(List<CardInstance> cards) {
        Set<String> ids = new HashSet<>();
        for (CardInstance card : cards) {
            Objects.requireNonNull(card, "market card");
            if (!ids.add(card.instanceId())) {
                throw new IllegalArgumentException("Market cards must have unique instance identifiers: " + card.instanceId());
            }
        }
    }
}
