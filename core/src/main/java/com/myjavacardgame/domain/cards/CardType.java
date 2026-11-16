package com.myjavacardgame.domain.cards;

import java.util.Objects;

/** Identifies a data-defined card type without fixing a catalog in code.
 *
 * @param id nonblank type identifier
 */
public record CardType(String id) {
    /**
     * Creates a card type.
     *
     * @param id nonblank type identifier
     * @throws NullPointerException if {@code id} is null
     * @throws IllegalArgumentException if {@code id} is blank
     */
    public CardType {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Card type id must not be blank");
        }
    }
}
