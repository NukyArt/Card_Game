package com.myjavacardgame.domain.round;

import java.util.Objects;
import java.util.Optional;

/**
 * The outcome of attempting to execute a phase command.
 *
 * @param accepted whether the command was accepted by the current phase
 * @param rejectionReason a nonblank reason when the command was rejected, otherwise empty
 */
public record PhaseCommandResult(boolean accepted, Optional<String> rejectionReason) {
    /**
     * Validates the relationship between a command outcome and its optional rejection reason.
     *
     * @throws NullPointerException if {@code rejectionReason} is null
     * @throws IllegalArgumentException if an accepted result has a reason or a rejected result
     *                                  has no nonblank reason
     */
    public PhaseCommandResult {
        Objects.requireNonNull(rejectionReason, "rejectionReason");
        if (accepted && rejectionReason.isPresent()) {
            throw new IllegalArgumentException("An accepted command must not have a rejection reason");
        }
        if (!accepted && (rejectionReason.isEmpty() || rejectionReason.orElseThrow().isBlank())) {
            throw new IllegalArgumentException("A rejected command must have a nonblank rejection reason");
        }
    }

    /**
     * Creates an accepted command outcome.
     *
     * @return an accepted outcome without a rejection reason
     */
    public static PhaseCommandResult accept() {
        return new PhaseCommandResult(true, Optional.empty());
    }

    /**
     * Creates a rejected command outcome.
     *
     * @param reason a nonblank explanation of why the command was rejected
     * @return a rejected outcome containing {@code reason}
     * @throws NullPointerException if {@code reason} is null
     * @throws IllegalArgumentException if {@code reason} is blank
     */
    public static PhaseCommandResult reject(String reason) {
        Objects.requireNonNull(reason, "reason");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason must not be blank");
        }
        return new PhaseCommandResult(false, Optional.of(reason));
    }
}
