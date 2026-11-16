package com.myjavacardgame.domain.round;

import java.util.Objects;

/**
 * Requests one normal, session-managed phase transition.
 *
 * <p>{@link PhaseCommand#END_MATCH} is intentionally not accepted through this command because
 * a session outcome is fixed only by {@link ProjectCompletedCommand} or the final deposit.</p>
 *
 * @param phaseCommand requested normal phase transition
 */
public record SessionPhaseCommand(PhaseCommand phaseCommand) implements GameSessionCommand {
    /**
     * Validates the requested phase command.
     *
     * @throws NullPointerException if {@code phaseCommand} is null
     */
    public SessionPhaseCommand {
        Objects.requireNonNull(phaseCommand, "phaseCommand");
    }
}
