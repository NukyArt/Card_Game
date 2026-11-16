package com.myjavacardgame.domain.round;

import java.util.Objects;

/**
 * Requests a gold credit to one participant without specifying its source.
 *
 * @param participantId participant receiving the gold
 * @param amount nonnegative amount of gold
 */
public record CreditGoldCommand(String participantId, int amount) implements GameSessionCommand {
    /**
     * Validates the requested credit.
     *
     * @throws NullPointerException if the participant identifier is null
     * @throws IllegalArgumentException if the identifier is blank or the amount is negative
     */
    public CreditGoldCommand {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.isBlank() || amount < 0) {
            throw new IllegalArgumentException("Participant identifier must not be blank and amount must not be negative");
        }
    }
}
