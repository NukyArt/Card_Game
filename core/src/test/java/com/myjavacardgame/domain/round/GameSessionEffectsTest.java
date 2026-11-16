package com.myjavacardgame.domain.round;

import static org.junit.jupiter.api.Assertions.*;

import com.myjavacardgame.application.DomainEventDispatcher;
import com.myjavacardgame.domain.board.BoardSnapshot;
import com.myjavacardgame.domain.cards.CardCatalog;
import com.myjavacardgame.domain.cards.CardDefinition;
import com.myjavacardgame.domain.cards.CardRole;
import com.myjavacardgame.domain.cards.CardRoleParameters;
import com.myjavacardgame.domain.cards.CardType;
import com.myjavacardgame.domain.effects.*;
import com.myjavacardgame.domain.events.EffectResolvedEvent;
import com.myjavacardgame.domain.events.GoldCreditedEvent;
import com.myjavacardgame.domain.events.DomainEvent;
import com.myjavacardgame.domain.project.ProjectDefinition;
import com.myjavacardgame.domain.project.ProjectStageCompletedEvent;
import com.myjavacardgame.domain.project.ProjectStageDefinition;
import com.myjavacardgame.domain.project.ProjectStageRequirements;
import com.myjavacardgame.domain.resources.ResourceType;
import java.util.ArrayList;
import com.myjavacardgame.domain.party.ParticipantType;
import com.myjavacardgame.domain.party.StartingPartyDefinition;
import com.myjavacardgame.domain.party.StartingPartyParticipantSpecification;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameSessionEffectsTest {
    private static final String HUMAN = "human";
    private static final String BOT = "bot-one";
    private static final int START_GOLD = 10;
    private static final int FIRST_CREDIT = 2;
    private static final int SECOND_CREDIT = 3;
    private static final String SOURCE = "source";

    @Test
    void creditsGoldAndReturnsOrderedImmutableEvents() {
        GameSession session = session();
        GameSessionCommandResult result = session.resolveEffect(request("effect", EffectTiming.IMMEDIATE,
                new CreditGoldOperation(HUMAN, FIRST_CREDIT), new CreditGoldOperation(BOT, SECOND_CREDIT)));
        assertTrue(result.accepted());
        assertEquals(START_GOLD + FIRST_CREDIT, result.snapshot().goldBalances().get(HUMAN));
        assertEquals(List.of(new GoldCreditedEvent(HUMAN, FIRST_CREDIT),
                new GoldCreditedEvent(BOT, SECOND_CREDIT), new EffectResolvedEvent("effect", SOURCE)), result.events());
        assertThrows(UnsupportedOperationException.class, () -> result.events().clear());
    }

    @Test
    void unknownRecipientRejectsAllCreditsAndEvents() {
        GameSession session = session();
        GameSessionSnapshot before = session.snapshot();
        GameSessionCommandResult result = session.resolveEffect(request("effect", EffectTiming.IMMEDIATE,
                new CreditGoldOperation(HUMAN, FIRST_CREDIT), new CreditGoldOperation("unknown", SECOND_CREDIT)));
        assertFalse(result.accepted());
        assertEquals(before, result.snapshot());
        assertTrue(result.events().isEmpty());
    }

    @Test
    void cumulativeOverflowRejectsAllCredits() {
        GameSession session = session();
        assertTrue(session.execute(new CreditGoldCommand(HUMAN, Integer.MAX_VALUE - START_GOLD - FIRST_CREDIT)).accepted());
        GameSessionSnapshot before = session.snapshot();
        GameSessionCommandResult result = session.resolveEffect(request("effect", EffectTiming.IMMEDIATE,
                new CreditGoldOperation(HUMAN, FIRST_CREDIT), new CreditGoldOperation(HUMAN, SECOND_CREDIT)));
        assertFalse(result.accepted());
        assertEquals(before, result.snapshot());
        assertTrue(result.events().isEmpty());
    }

    @Test
    void persistentEffectsApplyInInsertionOrder() {
        GameSession session = session();
        assertTrue(session.resolveEffect(request("first", EffectTiming.PERMANENT, new CreditGoldOperation(HUMAN, FIRST_CREDIT))).accepted());
        assertTrue(session.resolveEffect(request("second", EffectTiming.PERMANENT, new CreditGoldOperation(BOT, SECOND_CREDIT))).accepted());
        GameSessionCommandResult result = session.applyPersistentEffects();
        assertTrue(result.accepted());
        assertEquals(List.of(new GoldCreditedEvent(HUMAN, FIRST_CREDIT), new GoldCreditedEvent(BOT, SECOND_CREDIT)), result.events());
        assertEquals(List.of("first", "second"), result.snapshot().effectState().active().stream().map(e -> e.definition().id()).toList());
    }

    @Test
    void deliversStageCompletionAfterContributionAndNotAfterRejection() {
        StartingPartyDefinition party = party();
        ProjectStageRequirements requirements = new ProjectStageRequirements(Map.of(
                ResourceType.GOLD, FIRST_CREDIT, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        ProjectDefinition project = new ProjectDefinition("project", "Project", List.of(
                new ProjectStageDefinition("stage", "Stage", requirements, "None")));
        Map<String, BoardSnapshot> boards = Map.of(HUMAN, BoardSnapshot.empty(), BOT, BoardSnapshot.empty(),
                "bot-two", BoardSnapshot.empty(), "bot-three", BoardSnapshot.empty());
        GameSession session = new GameSession(new GameSessionDefinition(party, 2, new DepositRules(3, 5), project, boards));
        session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME));
        for (String participant : List.of(HUMAN, BOT, "bot-two", "bot-three")) session.execute(new EndTurnCommand(participant));
        session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_MARKET));
        List<DomainEvent> received = new ArrayList<>();
        DomainEventDispatcher dispatcher = new DomainEventDispatcher(List.of(received::add));

        GameSessionCommandResult result = session.execute(new ContributeGoldCommand(HUMAN, FIRST_CREDIT));
        assertTrue(result.accepted());
        assertEquals(List.of(new ProjectStageCompletedEvent("project", "stage", java.util.Optional.empty(), true)), result.events());
        assertTrue(dispatcher.dispatch(result).isEmpty());
        assertEquals(result.events(), received);
        GameSessionCommandResult rejected = session.execute(new ContributeGoldCommand(HUMAN, FIRST_CREDIT));
        assertFalse(rejected.accepted());
        dispatcher.dispatch(rejected);
        assertEquals(result.events(), received);
    }

    private static EffectResolutionRequest request(String id, EffectTiming timing, CreditGoldOperation... operations) {
        EffectDefinition definition = new EffectDefinition(id, timing, EffectCondition.ALWAYS, List.of(operations), List.of());
        return new EffectResolutionRequest(SOURCE, definition, List.of(), bound -> 0);
    }

    private static GameSession session() {
        return new GameSession(new GameSessionDefinition(party(), 2, new DepositRules(3, 5)));
    }

    private static StartingPartyDefinition party() {
        CardDefinition base = new CardDefinition("treasury", "Treasury", new CardType("starter"),
                Map.of(CardRole.BASE, new CardRoleParameters(Map.of())), "No effect");
        CardCatalog catalog = new CardCatalog(Map.of(base.id(), base));
        List<StartingPartyParticipantSpecification> participants = List.of(
                participant(HUMAN, ParticipantType.HUMAN), participant(BOT, ParticipantType.BOT),
                participant("bot-two", ParticipantType.BOT), participant("bot-three", ParticipantType.BOT));
        return StartingPartyDefinition.fromSpecifications(participants, catalog);
    }

    private static StartingPartyParticipantSpecification participant(String id, ParticipantType type) {
        return new StartingPartyParticipantSpecification(id, type, START_GOLD, List.of("treasury"));
    }
}
