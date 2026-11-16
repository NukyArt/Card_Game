package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.events.DomainEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable outcome of a {@link GameSession} command, including the session state after handling it.
 *
 * @param accepted whether the command was applied
 * @param rejectionReason empty for acceptance; a nonblank reason for rejection
 * @param snapshot immutable state after the accepted command or unchanged state after rejection
 */
public record GameSessionCommandResult(
        boolean accepted, Optional<String> rejectionReason, GameSessionSnapshot snapshot, List<DomainEvent> events) {
    /** Compatibility constructor for commands without events. */
    public GameSessionCommandResult(boolean accepted, Optional<String> rejectionReason, GameSessionSnapshot snapshot) {
        this(accepted, rejectionReason, snapshot, List.of());
    }
    /**
     * Validates the command outcome and exposed snapshot.
     *
     * @throws NullPointerException if {@code rejectionReason} or {@code snapshot} is null
     * @throws IllegalArgumentException if acceptance and the rejection reason disagree
     */
    public GameSessionCommandResult {
        Objects.requireNonNull(rejectionReason, "rejectionReason");
        Objects.requireNonNull(snapshot, "snapshot");
        events = List.copyOf(Objects.requireNonNull(events, "events"));
        if (!accepted && !events.isEmpty()) throw new IllegalArgumentException("Rejected commands cannot emit events");
        if (accepted && rejectionReason.isPresent()) {
            throw new IllegalArgumentException("An accepted command must not have a rejection reason");
        }
        if (!accepted && (rejectionReason.isEmpty() || rejectionReason.orElseThrow().isBlank())) {
            throw new IllegalArgumentException("A rejected command must have a nonblank rejection reason");
        }
    }

    /**
     * Creates an accepted result for a session state.
     *
     * @param snapshot immutable state after execution
     * @return accepted result without a rejection reason
     */
    public static GameSessionCommandResult accept(GameSessionSnapshot snapshot) {
        return new GameSessionCommandResult(true, Optional.empty(), snapshot);
    }

    /**
     * Creates a rejected result for an unchanged session state.
     *
     * @param reason nonblank rejection explanation
     * @param snapshot immutable unchanged session state
     * @return rejected result containing the reason
     */
    public static GameSessionCommandResult reject(String reason, GameSessionSnapshot snapshot) {
        Objects.requireNonNull(reason, "reason");
        if (reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason must not be blank");
        }
        return new GameSessionCommandResult(false, Optional.of(reason), snapshot);
    }
}
