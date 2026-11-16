package com.myjavacardgame.domain.round;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RoundPhaseMachineTest {
    @Test
    void completesTheNormalRoundCycleBackToIncome() {
        // Arrange
        RoundPhaseMachine machine = new RoundPhaseMachine();

        // Act / Assert
        assertAcceptedAndPhase(machine, PhaseCommand.COMPLETE_INCOME, RoundPhase.MARKET);
        assertAcceptedAndPhase(machine, PhaseCommand.COMPLETE_MARKET, RoundPhase.PRODUCTION);
        assertAcceptedAndPhase(machine, PhaseCommand.COMPLETE_PRODUCTION, RoundPhase.DEPOSIT);
        assertAcceptedAndPhase(machine, PhaseCommand.COMPLETE_DEPOSIT, RoundPhase.INCOME);
    }

    @Test
    void endsTheMatchFromMarketWithTheSeparateEndMatchCommand() {
        // Arrange
        RoundPhaseMachine machine = new RoundPhaseMachine();
        machine.execute(PhaseCommand.COMPLETE_INCOME);

        // Act
        PhaseCommandResult result = machine.execute(PhaseCommand.END_MATCH);

        // Assert
        assertTrue(result.accepted());
        assertTrue(result.rejectionReason().isEmpty());
        assertEquals(RoundPhase.RESULT, machine.currentPhase());
    }

    @Test
    void endsTheMatchFromProductionWithTheSeparateEndMatchCommand() {
        // Arrange
        RoundPhaseMachine machine = new RoundPhaseMachine();
        machine.execute(PhaseCommand.COMPLETE_INCOME);
        machine.execute(PhaseCommand.COMPLETE_MARKET);

        // Act
        PhaseCommandResult result = machine.execute(PhaseCommand.END_MATCH);

        // Assert
        assertTrue(result.accepted());
        assertTrue(result.rejectionReason().isEmpty());
        assertEquals(RoundPhase.RESULT, machine.currentPhase());
    }

    @Test
    void endsTheMatchFromDeposit() {
        // Arrange
        RoundPhaseMachine machine = new RoundPhaseMachine();
        machine.execute(PhaseCommand.COMPLETE_INCOME);
        machine.execute(PhaseCommand.COMPLETE_MARKET);
        machine.execute(PhaseCommand.COMPLETE_PRODUCTION);

        // Act
        PhaseCommandResult result = machine.execute(PhaseCommand.END_MATCH);

        // Assert
        assertTrue(result.accepted());
        assertEquals(RoundPhase.RESULT, machine.currentPhase());
    }

    @Test
    void rejectsACommandThatIsNotAllowedInTheCurrentPhaseWithoutChangingPhase() {
        // Arrange
        RoundPhaseMachine machine = new RoundPhaseMachine();
        RoundPhase phaseBeforeRejection = machine.currentPhase();

        // Act
        PhaseCommandResult result = machine.execute(PhaseCommand.COMPLETE_MARKET);

        // Assert
        assertFalse(result.accepted());
        assertEquals(
                "Command COMPLETE_MARKET is not allowed during INCOME phase.",
                result.rejectionReason().orElseThrow());
        assertEquals(phaseBeforeRejection, machine.currentPhase());
    }

    @Test
    void resultPhaseAllowsNoFurtherCommandsAndRejectsWithoutChangingPhase() {
        // Arrange
        RoundPhaseMachine machine = new RoundPhaseMachine();
        machine.execute(PhaseCommand.COMPLETE_INCOME);
        machine.execute(PhaseCommand.COMPLETE_MARKET);
        machine.execute(PhaseCommand.END_MATCH);

        // Act
        PhaseCommandResult result = machine.execute(PhaseCommand.COMPLETE_DEPOSIT);

        // Assert
        assertFalse(result.accepted());
        assertEquals(
                "Command COMPLETE_DEPOSIT is not allowed during RESULT phase.",
                result.rejectionReason().orElseThrow());
        assertEquals(RoundPhase.RESULT, machine.currentPhase());
    }

    private static void assertAcceptedAndPhase(
            RoundPhaseMachine machine, PhaseCommand command, RoundPhase expectedPhase) {
        PhaseCommandResult result = machine.execute(command);

        assertTrue(result.accepted());
        assertTrue(result.rejectionReason().isEmpty());
        assertEquals(expectedPhase, machine.currentPhase());
    }
}
