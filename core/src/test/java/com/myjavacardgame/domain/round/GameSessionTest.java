package com.myjavacardgame.domain.round;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myjavacardgame.domain.party.ParticipantType;
import com.myjavacardgame.domain.party.StartingPartyDefinition;
import com.myjavacardgame.domain.party.StartingPartyParticipantSpecification;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSessionTest {
    private static final List<String> PARTICIPANT_IDS = List.of("human", "bot-one", "bot-two", "bot-three");
    private static final List<String> STARTING_BASE_IDS = List.of("treasury");
    private static final DepositRules DEPOSIT_RULES = new DepositRules(3, 5, 2);

    @Test
    void createsAnImmutableSessionForOneHumanAndThreeBotsStartingWithTheHuman() {
        GameSession session = new GameSession(definition(2));

        GameSessionSnapshot snapshot = session.snapshot();

        assertEquals(PARTICIPANT_IDS, snapshot.participants().stream().map(SessionParticipantSnapshot::id).toList());
        assertEquals(
                List.of(ParticipantType.HUMAN, ParticipantType.BOT, ParticipantType.BOT, ParticipantType.BOT),
                snapshot.participants().stream().map(SessionParticipantSnapshot::type).toList());
        assertEquals("human", snapshot.firstParticipantId());
        assertEquals("human", snapshot.currentParticipantId());
        assertEquals(1, snapshot.roundNumber());
        assertEquals(RoundPhase.INCOME, snapshot.currentPhase());
        assertEquals(MatchOutcome.IN_PROGRESS, snapshot.outcome());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.participants().clear());
    }

    @Test
    void rejectsStartingDefinitionsOtherThanOneHumanAndThreeBots() {
        StartingPartyDefinition party = party(
                participant("human", ParticipantType.HUMAN),
                participant("bot-one", ParticipantType.BOT));

        assertThrows(IllegalArgumentException.class, () -> new GameSessionDefinition(party, 2, DEPOSIT_RULES));
    }

    @Test
    void marketAndProductionGiveEveryParticipantOneTurnInFixedOrder() {
        GameSession session = new GameSession(definition(2));
        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)));

        assertEndTurns(session, PARTICIPANT_IDS);
        assertRejected(session.execute(new EndTurnCommand("human")), session.snapshot());
        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_MARKET)));
        assertEquals(RoundPhase.PRODUCTION, session.snapshot().currentPhase());
        assertEquals("human", session.snapshot().currentParticipantId());

        assertEndTurns(session, PARTICIPANT_IDS);
        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_PRODUCTION)));
        assertEquals(RoundPhase.DEPOSIT, session.snapshot().currentPhase());
    }

    @Test
    void rejectsAnOutOfTurnEndTurnAtomically() {
        GameSession session = new GameSession(definition(2));
        session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME));
        GameSessionSnapshot before = session.snapshot();

        GameSessionCommandResult result = session.execute(new EndTurnCommand("bot-one"));

        assertRejected(result, before);
    }

    @Test
    void rotatesFirstParticipantAndIncrementsRoundOnlyAfterDepositCompletes() {
        GameSession session = new GameSession(definition(2));
        completeRound(session);
        GameSessionSnapshot beforeDeposit = session.snapshot();
        assertEquals(1, beforeDeposit.roundNumber());
        assertEquals("human", beforeDeposit.firstParticipantId());

        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_DEPOSIT)));

        GameSessionSnapshot snapshot = session.snapshot();
        assertEquals(2, snapshot.roundNumber());
        assertEquals("bot-one", snapshot.firstParticipantId());
        assertEquals("bot-one", snapshot.currentParticipantId());
        assertEquals(RoundPhase.INCOME, snapshot.currentPhase());
        assertEquals(MatchOutcome.IN_PROGRESS, snapshot.outcome());
    }

    @Test
    void recordsDefeatAfterFinalDepositWithoutEnteringTheNextRound() {
        GameSession session = new GameSession(definition(1));
        completeRound(session);

        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_DEPOSIT)));

        GameSessionSnapshot snapshot = session.snapshot();
        assertEquals(1, snapshot.roundNumber());
        assertEquals("human", snapshot.firstParticipantId());
        assertEquals(RoundPhase.RESULT, snapshot.currentPhase());
        assertEquals(MatchOutcome.DEFEAT, snapshot.outcome());
    }

    @Test
    void projectCompletionRecordsVictoryImmediatelyDuringMarketOrProduction() {
        GameSession marketSession = new GameSession(definition(2));
        marketSession.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME));
        assertAccepted(marketSession.execute(new ProjectCompletedCommand()));
        assertEquals(MatchOutcome.VICTORY, marketSession.snapshot().outcome());
        assertEquals(RoundPhase.RESULT, marketSession.snapshot().currentPhase());

        GameSession productionSession = new GameSession(definition(2));
        productionSession.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME));
        assertEndTurns(productionSession, PARTICIPANT_IDS);
        productionSession.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_MARKET));
        assertAccepted(productionSession.execute(new ProjectCompletedCommand()));
        assertEquals(MatchOutcome.VICTORY, productionSession.snapshot().outcome());
        assertEquals(RoundPhase.RESULT, productionSession.snapshot().currentPhase());
    }

    @Test
    void rejectsCommandsAfterTheMatchWithoutChangingItsSnapshot() {
        GameSession session = new GameSession(definition(2));
        session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME));
        session.execute(new ProjectCompletedCommand());
        GameSessionSnapshot before = session.snapshot();

        assertRejected(session.execute(new EndTurnCommand("human")), before);
        assertRejected(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_MARKET)), before);
        assertRejected(session.execute(new ProjectCompletedCommand()), before);
    }

    @Test
    void paysRoundIncomeToEveryParticipantAfterDeposit() {
        GameSession session = new GameSession(definition(2));
        assertTrue(session.snapshot().goldBalances().values().stream().allMatch(gold -> gold == 10));
        completeRound(session);
        assertTrue(session.snapshot().goldBalances().values().stream().allMatch(gold -> gold == 10));

        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_DEPOSIT)));

        // Ten saved coins earn three deposit coins, followed by the two-coin round income.
        assertTrue(session.snapshot().goldBalances().values().stream().allMatch(gold -> gold == 15));
        GameSessionSnapshot paid = session.snapshot();
        assertRejected(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_DEPOSIT)), paid);
        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)));
        assertEquals(paid.goldBalances(), session.snapshot().goldBalances());
    }

    @Test
    void paysIncomeEvenWhenEveryParticipantHasNoGold() {
        GameSession session = new GameSession(definitionWithGold(2, 0));
        completeRound(session);
        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_DEPOSIT)));
        assertTrue(session.snapshot().goldBalances().values().stream().allMatch(gold -> gold == 2));
    }

    @Test
    void calculatesDepositBeforeRoundIncome() {
        GameSession session = new GameSession(definitionWithGold(2, 1));
        completeRound(session);
        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_DEPOSIT)));
        // The new income must not bring the saved balance to the three-coin deposit threshold.
        assertTrue(session.snapshot().goldBalances().values().stream().allMatch(gold -> gold == 3));
    }

    @Test
    void paysIncomeInSuccessiveRoundsIncludingTheFinalRound() {
        GameSession session = new GameSession(definitionWithGold(2, 0));
        for (int round = 1; round <= 2; round++) {
            assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)));
            for (PhaseCommand command : List.of(PhaseCommand.COMPLETE_MARKET, PhaseCommand.COMPLETE_PRODUCTION)) {
                for (int turn = 0; turn < PARTICIPANT_IDS.size(); turn++) {
                    assertAccepted(session.execute(new EndTurnCommand(session.snapshot().currentParticipantId())));
                }
                assertAccepted(session.execute(new SessionPhaseCommand(command)));
            }
            assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_DEPOSIT)));
            int expectedGold = round * 2;
            assertTrue(session.snapshot().goldBalances().values().stream().allMatch(gold -> gold == expectedGold));
        }
        assertEquals(MatchOutcome.DEFEAT, session.snapshot().outcome());
    }

    @Test
    void rejectsRoundPayoutOverflowWithoutChangingAnyState() {
        GameSession session = new GameSession(definitionWithGold(2, Integer.MAX_VALUE - 6));
        completeRound(session);
        GameSessionSnapshot before = session.snapshot();
        assertRejected(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_DEPOSIT)), before);
    }

    @Test
    void acceptsRoundPayoutAtTheMaximumSupportedBalance() {
        GameSession session = new GameSession(definitionWithGold(2, Integer.MAX_VALUE - 7));
        completeRound(session);
        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_DEPOSIT)));
        assertTrue(session.snapshot().goldBalances().values().stream().allMatch(gold -> gold == Integer.MAX_VALUE));
    }

    @Test
    void validatesIncomeAndSupportsExplicitDepositOnlyRules() {
        assertThrows(IllegalArgumentException.class, () -> new DepositRules(3, 5, -1));
        assertEquals(0, new DepositRules(3, 5).roundIncomeGold());
    }

    private static GameSessionDefinition definitionWithGold(int roundLimit, int gold) {
        return new GameSessionDefinition(StartingPartyDefinition.fromSpecifications(PARTICIPANT_IDS.stream()
                .map(id -> new StartingPartyParticipantSpecification(id,
                        id.equals("human") ? ParticipantType.HUMAN : ParticipantType.BOT, gold, STARTING_BASE_IDS))
                .toList(), starterCatalog()), roundLimit, DEPOSIT_RULES);
    }

    private static void completeRound(GameSession session) {
        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)));
        assertEndTurns(session, PARTICIPANT_IDS);
        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_MARKET)));
        assertEndTurns(session, PARTICIPANT_IDS);
        assertAccepted(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_PRODUCTION)));
    }

    private static void assertEndTurns(GameSession session, List<String> participantIds) {
        for (String participantId : participantIds) {
            assertAccepted(session.execute(new EndTurnCommand(participantId)));
        }
    }

    private static void assertAccepted(GameSessionCommandResult result) {
        assertTrue(result.accepted());
        assertTrue(result.rejectionReason().isEmpty());
    }

    private static void assertRejected(GameSessionCommandResult result, GameSessionSnapshot expectedSnapshot) {
        assertFalse(result.accepted());
        assertTrue(result.rejectionReason().isPresent());
        assertEquals(expectedSnapshot, result.snapshot());
    }

    private static GameSessionDefinition definition(int roundLimit) {
        return new GameSessionDefinition(party(
                participant("human", ParticipantType.HUMAN),
                participant("bot-one", ParticipantType.BOT),
                participant("bot-two", ParticipantType.BOT),
                participant("bot-three", ParticipantType.BOT)), roundLimit, DEPOSIT_RULES);
    }

    private static StartingPartyDefinition party(StartingPartyParticipantSpecification... participants) {
        return StartingPartyDefinition.fromSpecifications(List.of(participants), starterCatalog());
    }

    private static StartingPartyParticipantSpecification participant(String id, ParticipantType type) {
        return new StartingPartyParticipantSpecification(id, type, 10, STARTING_BASE_IDS);
    }

    private static com.myjavacardgame.domain.cards.CardCatalog starterCatalog() {
        com.myjavacardgame.domain.cards.CardDefinition base = new com.myjavacardgame.domain.cards.CardDefinition(
                "treasury",
                "Treasury",
                new com.myjavacardgame.domain.cards.CardType("starter"),
                java.util.Map.of(
                        com.myjavacardgame.domain.cards.CardRole.BASE,
                        new com.myjavacardgame.domain.cards.CardRoleParameters(java.util.Map.of())),
                "No effect");
        return new com.myjavacardgame.domain.cards.CardCatalog(java.util.Map.of(base.id(), base));
    }
}
