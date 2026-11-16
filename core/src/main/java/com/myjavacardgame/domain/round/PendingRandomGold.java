package com.myjavacardgame.domain.round;

import java.util.Objects;

/** Public view of a revealed random gold payout awaiting participant allocation. */
public record PendingRandomGold(String participantId, int amount, int remainingStageGold, int remainingDraws) {
    /** Validates the revealed payout and remaining work. */
    public PendingRandomGold {
        Objects.requireNonNull(participantId, "participantId");
        if (participantId.isBlank() || amount < 1 || remainingStageGold < 0 || remainingDraws < 0) {
            throw new IllegalArgumentException("Invalid pending random payout");
        }
    }
}
