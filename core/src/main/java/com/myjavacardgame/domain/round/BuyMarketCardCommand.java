package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.cards.CardRole;
import java.util.Objects;

/** Requests purchase of one face-up market card into a selected board role.
 *
 * @param participantId participant making the purchase
 * @param marketSlot zero-based market position
 * @param role selected permitted role for the purchased card
 * @param baseIndex target base for an upgrade, or {@code -1} for a base
 */
public record BuyMarketCardCommand(String participantId, int marketSlot, CardRole role, int baseIndex)
        implements GameSessionCommand {
    /** Validates the target shape without checking current market or board state. */
    public BuyMarketCardCommand {
        Objects.requireNonNull(participantId, "participantId");
        Objects.requireNonNull(role, "role");
        if (participantId.isBlank() || marketSlot < 0
                || (role == CardRole.BASE && baseIndex != -1)
                || (role == CardRole.UPGRADE && baseIndex < 0)) {
            throw new IllegalArgumentException("Market purchase target is invalid");
        }
    }
}
