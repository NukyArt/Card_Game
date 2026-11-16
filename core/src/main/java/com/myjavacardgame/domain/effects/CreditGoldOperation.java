package com.myjavacardgame.domain.effects;

import java.util.Objects;

/** Credits a positive amount of gold to a participant. */
public record CreditGoldOperation(String participantId, int amount) implements EffectOperation {
    /** Validates the recipient and amount. */
    public CreditGoldOperation {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.isBlank() || amount <= 0) throw new IllegalArgumentException("Invalid gold credit");
    }
}
