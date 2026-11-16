package com.myjavacardgame.domain.round;

/**
 * Signals that an external project domain has completed its final stage.
 *
 * <p>The signal carries no project-progress mechanics. {@link GameSession} only validates that
 * it arrived during a phase where the story permits an immediate victory.</p>
 */
public record ProjectCompletedCommand() implements GameSessionCommand {
}
