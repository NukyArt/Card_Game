package com.myjavacardgame.domain.round;

import java.util.Objects;
import java.util.Set;

/**
 * Stateful coordinator for the ordered phases of a round.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * RoundPhaseMachine machine = new RoundPhaseMachine();
 * PhaseCommandResult result = machine.execute(PhaseCommand.COMPLETE_INCOME);
 * if (result.accepted()) {
 *     assert machine.currentPhase() == RoundPhase.MARKET;
 * }
 * }</pre>
 *
 * <p>This class only validates and applies phase transitions. It does not determine a match
 * outcome, track participants or round limits, execute game actions, or read system time.</p>
 */
public final class RoundPhaseMachine {
    private static final PhaseState INCOME_STATE = new IncomePhaseState();
    private static final PhaseState MARKET_STATE = new MarketPhaseState();
    private static final PhaseState PRODUCTION_STATE = new ProductionPhaseState();
    private static final PhaseState DEPOSIT_STATE = new DepositPhaseState();
    private static final PhaseState RESULT_STATE = new ResultPhaseState();

    private PhaseState currentState = INCOME_STATE;

    /**
     * Creates a phase machine at the income phase of a round.
     */
    public RoundPhaseMachine() {
        // The initial state is declared with the field initializer.
    }

    /**
     * Returns the current round phase.
     *
     * @return the phase represented by the current state
     */
    public RoundPhase currentPhase() {
        return currentState.phase();
    }

    /**
     * Attempts to apply a command to the current phase.
     *
     * <p>If the command is not allowed by the current phase, this method returns a rejected
     * result and leaves the current phase unchanged.</p>
     *
     * @param command the requested phase transition
     * @return an accepted result after a transition, or a rejected result with its reason
     * @throws NullPointerException if {@code command} is null
     */
    public PhaseCommandResult execute(PhaseCommand command) {
        Objects.requireNonNull(command, "command");
        if (!currentState.accepts(command)) {
            return PhaseCommandResult.reject(
                    "Command " + command + " is not allowed during " + currentState.phase() + " phase.");
        }

        currentState = currentState.transition(command);
        return PhaseCommandResult.accept();
    }

    private abstract static class AbstractPhaseState implements PhaseState {
        private final RoundPhase phase;
        private final Set<PhaseCommand> allowedCommands;

        private AbstractPhaseState(RoundPhase phase, Set<PhaseCommand> allowedCommands) {
            this.phase = phase;
            this.allowedCommands = allowedCommands;
        }

        @Override
        public final RoundPhase phase() {
            return phase;
        }

        @Override
        public final Set<PhaseCommand> allowedCommands() {
            return allowedCommands;
        }

        @Override
        public final boolean accepts(PhaseCommand command) {
            return allowedCommands.contains(command);
        }

        protected final void requireAccepted(PhaseCommand command) {
            Objects.requireNonNull(command, "command");
            if (!accepts(command)) {
                throw new IllegalArgumentException(
                        "Command " + command + " is not allowed during " + phase + " phase.");
            }
        }
    }

    private static final class IncomePhaseState extends AbstractPhaseState {
        private IncomePhaseState() {
            super(RoundPhase.INCOME, Set.of(PhaseCommand.COMPLETE_INCOME));
        }

        @Override
        public PhaseState transition(PhaseCommand command) {
            requireAccepted(command);
            return MARKET_STATE;
        }
    }

    private static final class MarketPhaseState extends AbstractPhaseState {
        private MarketPhaseState() {
            super(RoundPhase.MARKET, Set.of(PhaseCommand.COMPLETE_MARKET, PhaseCommand.END_MATCH));
        }

        @Override
        public PhaseState transition(PhaseCommand command) {
            requireAccepted(command);
            return command == PhaseCommand.COMPLETE_MARKET ? PRODUCTION_STATE : RESULT_STATE;
        }
    }

    private static final class ProductionPhaseState extends AbstractPhaseState {
        private ProductionPhaseState() {
            super(RoundPhase.PRODUCTION, Set.of(PhaseCommand.COMPLETE_PRODUCTION, PhaseCommand.END_MATCH));
        }

        @Override
        public PhaseState transition(PhaseCommand command) {
            requireAccepted(command);
            return command == PhaseCommand.COMPLETE_PRODUCTION ? DEPOSIT_STATE : RESULT_STATE;
        }
    }

    private static final class DepositPhaseState extends AbstractPhaseState {
        private DepositPhaseState() {
            super(RoundPhase.DEPOSIT, Set.of(PhaseCommand.COMPLETE_DEPOSIT, PhaseCommand.END_MATCH));
        }

        @Override
        public PhaseState transition(PhaseCommand command) {
            requireAccepted(command);
            return command == PhaseCommand.COMPLETE_DEPOSIT ? INCOME_STATE : RESULT_STATE;
        }
    }

    private static final class ResultPhaseState extends AbstractPhaseState {
        private ResultPhaseState() {
            super(RoundPhase.RESULT, Set.of());
        }

        @Override
        public PhaseState transition(PhaseCommand command) {
            requireAccepted(command);
            throw new IllegalStateException("The result phase has no successor state");
        }
    }
}
