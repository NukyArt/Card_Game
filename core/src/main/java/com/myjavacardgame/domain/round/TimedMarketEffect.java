package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.cards.CardType;
import java.util.Objects;
import java.util.Optional;

/** A market entitlement bound to its owner and applicable round. */
public record TimedMarketEffect(Kind kind, String participantId, int round, int amount, Optional<CardType> cardType) {
    /** Supported timed market entitlements. */
    public enum Kind { SALE_BONUS, PURCHASE_DISCOUNT }

    /** Validates the owner, expiry round and effect parameters. */
    public TimedMarketEffect {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(participantId, "participantId");
        Objects.requireNonNull(cardType, "cardType");
        if (participantId.isBlank() || round < 1 || amount < 0
                || (kind == Kind.PURCHASE_DISCOUNT) != cardType.isPresent()) {
            throw new IllegalArgumentException("Invalid timed market effect");
        }
    }
}
