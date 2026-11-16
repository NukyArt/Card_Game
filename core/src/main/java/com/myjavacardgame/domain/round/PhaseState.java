package com.myjavacardgame.domain.round;

import java.util.Set;

/**
 * State Pattern contract for one round phase.
 *
 * <p>A phase state owns the commands it permits and the successor state for each permitted
 * command. {@link RoundPhaseMachine} checks command validity before requesting a transition.</p>
 */
public interface PhaseState {
    /**
     * Returns the phase represented by this state.
     *
     * @return the represented phase
     */
    RoundPhase phase();

    /**
     * Returns the commands permitted while this state is current.
     *
     * @return an immutable set of permitted commands
     */
    Set<PhaseCommand> allowedCommands();

    /**
     * Determines whether this state permits a command.
     *
     * @param command the command to evaluate
     * @return {@code true} when the command is permitted in this state
     */
    boolean accepts(PhaseCommand command);

    /**
     * Returns the successor state for a permitted command.
     *
     * @param command a command accepted by this state
     * @return the successor state
     * @throws IllegalArgumentException if the command is not accepted by this state
     */
    PhaseState transition(PhaseCommand command);
}
