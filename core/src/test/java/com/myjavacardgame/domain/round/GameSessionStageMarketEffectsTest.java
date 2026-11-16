package com.myjavacardgame.domain.round;

import static org.junit.jupiter.api.Assertions.*;

import com.myjavacardgame.domain.project.StageEffect;
import com.myjavacardgame.domain.project.ProjectDefinition;
import com.myjavacardgame.domain.project.ProjectStageDefinition;
import com.myjavacardgame.domain.project.ProjectStageRequirements;
import com.myjavacardgame.domain.board.BoardSnapshot;
import com.myjavacardgame.domain.cards.*;
import com.myjavacardgame.domain.party.*;
import java.util.List;
import java.util.Map;
import com.myjavacardgame.domain.resources.ResourceType;
import org.junit.jupiter.api.Test;

class GameSessionStageMarketEffectsTest {
    @Test
    void completingStageCreditsAllParticipants() {
        var requirements = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var next = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 1, ResourceType.SCROLLS, 1), 0);
        var first = new ProjectStageDefinition("one", "One", requirements, "Credit", List.of(),
                List.of(new StageEffect(StageEffect.Kind.CREDIT_EACH, ResourceType.GOLD, 2)));
        var project = new ProjectDefinition("project", "Project", List.of(first, new ProjectStageDefinition("two", "Two", next, "None")));
        var card = new CardDefinition("base", "Base", new CardType("type"), Map.of(CardRole.BASE, new CardRoleParameters(Map.of())), "None");
        var catalog = new CardCatalog(Map.of("base", card));
        var ids = List.of("human", "bot-one", "bot-two", "bot-three");
        var party = StartingPartyDefinition.fromSpecifications(ids.stream().map(id -> new StartingPartyParticipantSpecification(id,
                id.equals("human") ? ParticipantType.HUMAN : ParticipantType.BOT, 10, List.of("base"))).toList(), catalog);
        var boards = Map.of("human", BoardSnapshot.empty(), "bot-one", BoardSnapshot.empty(),
                "bot-two", BoardSnapshot.empty(), "bot-three", BoardSnapshot.empty());
        var session = new GameSession(new GameSessionDefinition(party, 2, new DepositRules(3, 5), project, boards));
        session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME));
        for (String id : ids) session.execute(new EndTurnCommand(id));
        session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_MARKET));
        var result = session.execute(new ContributeGoldCommand("human", 1));
        assertTrue(result.accepted());
        assertEquals(11, result.snapshot().goldBalances().get("human"));
        assertEquals(12, result.snapshot().goldBalances().get("bot-one"));
        assertEquals(5, result.events().size());
        assertInstanceOf(com.myjavacardgame.domain.project.ProjectStageCompletedEvent.class, result.events().get(0));
        assertInstanceOf(com.myjavacardgame.domain.events.GoldCreditedEvent.class, result.events().get(1));
        assertEquals("two", result.snapshot().projectProgress().orElseThrow().currentStageId().orElseThrow());
    }

    @Test
    void stageContributionEffectsChainWithoutCarryingExcess() {
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var second = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 0, ResourceType.PROVISIONS, 2,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var third = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 10,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var stages = List.of(
                new ProjectStageDefinition("first", "First", first, "Contribute", List.of(),
                        List.of(new StageEffect(StageEffect.Kind.CONTRIBUTE_EACH, ResourceType.PROVISIONS, 3))),
                new ProjectStageDefinition("second", "Second", second, "Credit", List.of(),
                        List.of(new StageEffect(StageEffect.Kind.CREDIT_EACH, ResourceType.GOLD, 2))),
                new ProjectStageDefinition("third", "Third", third, "None"));
        var progress = new com.myjavacardgame.domain.project.ProjectProgress(new ProjectDefinition("p", "P", stages));
        var state = new StageResolution(progress, Map.of("a", 10, "b", 10), List.of());
        state.contribute(ResourceType.GOLD, 1);
        assertEquals(List.of("first", "second"), state.events.stream().map(e -> e.completedStageId()).toList());
        assertEquals(3, state.progress.snapshot().contributedResources().get(ResourceType.PROVISIONS));
        assertEquals(12, state.balances.get("a"));
        assertEquals(0, progress.snapshot().completedStageCount());
    }

    @Test
    void stagePenaltyAppliesToLaterActivationInSession() {
        var requirements = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var next = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 10, ResourceType.PROVISIONS, 10,
                ResourceType.BUILDING_MATERIALS, 10, ResourceType.SCROLLS, 10), 0);
        var first = new ProjectStageDefinition("first", "First", requirements, "Penalty", List.of(),
                List.of(new StageEffect(StageEffect.Kind.MODIFY_PRODUCTION, ResourceType.BUILDING_MATERIALS, -1)));
        var project = new ProjectDefinition("p", "Project", List.of(first, new ProjectStageDefinition("next", "Next", next, "None")));
        var input = getClass().getResourceAsStream("/data/card-catalog.json");
        var catalog = new com.myjavacardgame.application.CardCatalogLoader().load(
                new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).catalog();
        var ids = List.of("human", "bot-one", "bot-two", "bot-three");
        var party = StartingPartyDefinition.fromSpecifications(ids.stream().map(id -> new StartingPartyParticipantSpecification(id,
                id.equals("human") ? ParticipantType.HUMAN : ParticipantType.BOT, 10, List.of("treasurer"))).toList(), catalog);
        var board = com.myjavacardgame.domain.board.BoardPlacement.placeBase(BoardSnapshot.empty(),
                CardInstance.fromCatalog("card", "sawmill", catalog), new com.myjavacardgame.domain.board.BoardRules(6, 3));
        var boards = Map.of("human", board, "bot-one", BoardSnapshot.empty(), "bot-two", BoardSnapshot.empty(), "bot-three", BoardSnapshot.empty());
        var session = new GameSession(new GameSessionDefinition(party, 3, new DepositRules(3, 5), project, boards));
        beginProduction(session);
        assertTrue(session.execute(new ContributeGoldCommand("human", 1)).accepted());
        assertEquals(1, session.snapshot().stageModifiers().size());
        var result = session.execute(new ActivateBaseCommand("human", 0, 0));
        assertTrue(result.accepted());
        assertEquals(3, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.BUILDING_MATERIALS));
    }

    @Test
    void quarryPurchasePayoutIgnoresStageProductionPenalty() {
        var input = getClass().getResourceAsStream("/data/card-catalog.json");
        var catalog = new com.myjavacardgame.application.CardCatalogLoader().load(
                new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).catalog();
        var ids = List.of("human", "bot-one", "bot-two", "bot-three");
        var party = StartingPartyDefinition.fromSpecifications(ids.stream().map(id -> new StartingPartyParticipantSpecification(id,
                id.equals("human") ? ParticipantType.HUMAN : ParticipantType.BOT, 10, List.of("treasurer"))).toList(), catalog);
        var rules = new com.myjavacardgame.domain.board.BoardRules(6, 3);
        var board = com.myjavacardgame.domain.board.BoardPlacement.placeBase(BoardSnapshot.empty(),
                CardInstance.fromCatalog("quarry", "quarry", catalog), rules);
        var boards = Map.of("human", board, "bot-one", BoardSnapshot.empty(), "bot-two", BoardSnapshot.empty(), "bot-three", BoardSnapshot.empty());
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var next = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 10, ResourceType.PROVISIONS, 10,
                ResourceType.BUILDING_MATERIALS, 10, ResourceType.SCROLLS, 10), 0);
        var project = new ProjectDefinition("p", "Project", List.of(
                new ProjectStageDefinition("first", "First", first, "Penalty", List.of(),
                        List.of(new StageEffect(StageEffect.Kind.MODIFY_PRODUCTION, ResourceType.BUILDING_MATERIALS, -1))),
                new ProjectStageDefinition("next", "Next", next, "None")));
        var market = new com.myjavacardgame.domain.market.MarketDeck(new com.myjavacardgame.domain.market.MarketRules(1, 2, rules, 2),
                List.of(CardInstance.fromCatalog("market", "coal-mine", catalog)), bound -> 0);
        var session = new GameSession(new GameSessionDefinition(party, 3, new DepositRules(3, 5), project, boards, market, Map.of()));
        beginProduction(session);
        assertTrue(session.execute(new ContributeGoldCommand("human", 1)).accepted());
        finishRound(session);
        for (String id : List.of("bot-one", "bot-two", "bot-three")) assertTrue(session.execute(new EndTurnCommand(id)).accepted());
        var result = session.execute(new BuyMarketCardCommand("human", 0, CardRole.BASE, -1));
        assertTrue(result.accepted());
        assertEquals(2, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.BUILDING_MATERIALS));
    }

    @Test
    void quarryPurchaseCompletesStageAndCreditsParticipants() {
        var stream = getClass().getResourceAsStream("/data/card-catalog.json");
        var catalog = new com.myjavacardgame.application.CardCatalogLoader().load(
                new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).catalog();
        var ids = List.of("human", "bot-one", "bot-two", "bot-three");
        var party = StartingPartyDefinition.fromSpecifications(ids.stream().map(id -> new StartingPartyParticipantSpecification(id,
                id.equals("human") ? ParticipantType.HUMAN : ParticipantType.BOT, 10, List.of("treasurer"))).toList(), catalog);
        var rules = new com.myjavacardgame.domain.board.BoardRules(6, 3);
        var board = com.myjavacardgame.domain.board.BoardPlacement.placeBase(BoardSnapshot.empty(),
                CardInstance.fromCatalog("quarry", "quarry", catalog), rules);
        var boards = Map.of("human", board, "bot-one", BoardSnapshot.empty(), "bot-two", BoardSnapshot.empty(), "bot-three", BoardSnapshot.empty());
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 0, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 2, ResourceType.SCROLLS, 0), 0);
        var next = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 10, ResourceType.PROVISIONS, 10,
                ResourceType.BUILDING_MATERIALS, 10, ResourceType.SCROLLS, 10), 0);
        var project = new ProjectDefinition("p", "Project", List.of(
                new ProjectStageDefinition("first", "First", first, "Credit", List.of(),
                        List.of(new StageEffect(StageEffect.Kind.CREDIT_EACH, ResourceType.GOLD, 2))),
                new ProjectStageDefinition("next", "Next", next, "None")));
        var market = new com.myjavacardgame.domain.market.MarketDeck(new com.myjavacardgame.domain.market.MarketRules(1, 2, rules, 2),
                List.of(CardInstance.fromCatalog("market", "coal-mine", catalog)), bound -> 0);
        var session = new GameSession(new GameSessionDefinition(party, 3, new DepositRules(3, 5), project, boards, market, Map.of()));
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)).accepted());
        var result = session.execute(new BuyMarketCardCommand("human", 0, CardRole.BASE, -1));
        assertTrue(result.accepted());
        assertEquals(8, result.snapshot().goldBalances().get("human"));
        assertEquals(12, result.snapshot().goldBalances().get("bot-one"));
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().completedStageCount());
    }

    @Test
    void rejectsPackageWhenStageHasNoCardDemand() {
        var session = marketSession("treasurer", "farm", 1, 0);
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)).accepted());
        var before = session.snapshot();
        var result = session.execute(new ContributeBoardPackageCommand("human", 0));
        assertFalse(result.accepted());
        assertEquals(before, result.snapshot());
        assertTrue(result.events().isEmpty());
    }

    @Test
    void rejectsAnotherPackageAfterCardDemandIsSatisfied() {
        var session = marketSession("treasurer", "farm", 2, 1);
        beginProduction(session);
        assertTrue(session.execute(new ContributeBoardPackageCommand("human", 0)).accepted());
        var before = session.snapshot();
        var result = session.execute(new ContributeBoardPackageCommand("human", 0));
        assertFalse(result.accepted());
        assertEquals(before, result.snapshot());
        assertTrue(result.events().isEmpty());
    }

    @Test
    void collectorBonusSurvivesRemovalBeforeSale() {
        var session = marketSession("tax-collector", "treasurer", 2);
        beginProduction(session);
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 0)).accepted());
        assertTrue(session.execute(new ContributeBoardPackageCommand("human", 0)).accepted());
        finishRound(session);
        for (String id : List.of("bot-one", "bot-two", "bot-three")) assertTrue(session.execute(new EndTurnCommand(id)).accepted());
        int before = session.snapshot().goldBalances().get("human");
        var result = session.execute(new SellBasePackageCommand("human", 0));
        assertTrue(result.accepted());
        assertEquals(before + 3, result.snapshot().goldBalances().get("human"));
    }

    @Test
    void stablesDiscountSurvivesRemovalBeforePurchase() {
        var session = marketSession("stables", "farm");
        beginProduction(session);
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 0)).accepted());
        assertTrue(session.execute(new ContributeBoardPackageCommand("human", 0)).accepted());
        finishRound(session);
        for (String id : List.of("bot-one", "bot-two", "bot-three")) assertTrue(session.execute(new EndTurnCommand(id)).accepted());
        int before = session.snapshot().goldBalances().get("human");
        var result = session.execute(new BuyMarketCardCommand("human", 0, CardRole.BASE, -1));
        assertTrue(result.accepted());
        assertEquals(before - 3, result.snapshot().goldBalances().get("human"));
    }

    @Test
    void observatoryEntitlementSurvivesCardContribution() {
        var session = marketSession("observatory", "farm");
        beginProduction(session);
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 0)).accepted());
        assertTrue(session.execute(new ContributeBoardPackageCommand("human", 0)).accepted());
        assertTrue(session.snapshot().boards().get("human").slots().isEmpty());
        finishRound(session);
        for (String id : List.of("bot-one", "bot-two", "bot-three")) assertTrue(session.execute(new EndTurnCommand(id)).accepted());
        int before = session.snapshot().goldBalances().get("human");
        var result = session.execute(new RefreshMarketCommand("human"));
        assertTrue(result.accepted());
        assertEquals(before, result.snapshot().goldBalances().get("human"));
    }

    @Test
    void collectorBonusExpiresAfterTargetMarketPhase() {
        var session = marketSession("tax-collector", "treasurer");
        beginProduction(session);
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 0)).accepted());
        assertEquals(1, session.snapshot().timedMarketEffects().size());
        finishRound(session);
        for (String id : List.of("bot-one", "bot-two", "bot-three", "human")) assertTrue(session.execute(new EndTurnCommand(id)).accepted());
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_MARKET)).accepted());
        assertTrue(session.snapshot().timedMarketEffects().isEmpty());
    }

    @Test
    void stackedCollectorBonusesApplyOncePerSale() {
        var session = marketSession("tax-collector", "treasurer", 2);
        beginProduction(session);
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 0)).accepted());
        assertTrue(session.execute(new ActivateBaseCommand("human", 1, 0)).accepted());
        finishRound(session);
        for (String id : List.of("bot-one", "bot-two", "bot-three")) assertTrue(session.execute(new EndTurnCommand(id)).accepted());
        int before = session.snapshot().goldBalances().get("human");
        var sale = session.execute(new SellBasePackageCommand("human", 0));
        assertTrue(sale.accepted());
        assertEquals(before + 4, sale.snapshot().goldBalances().get("human"));
    }

    @Test
    void multipleQuarriesStackForMatchingPurchase() {
        var session = marketSession("quarry", "coal-mine", 2);
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)).accepted());
        var result = session.execute(new BuyMarketCardCommand("human", 0, CardRole.BASE, -1));
        assertTrue(result.accepted());
        assertEquals(4, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.BUILDING_MATERIALS));
    }

    @Test
    void quarryDoesNotTriggerOnItsOwnPurchase() {
        var session = marketSession("sawmill", "quarry");
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)).accepted());
        var result = session.execute(new BuyMarketCardCommand("human", 0, CardRole.BASE, -1));
        assertTrue(result.accepted());
        assertEquals(0, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.BUILDING_MATERIALS));
    }

    @Test
    void stablesDiscountAppliesInNextMarketPhase() {
        var session = marketSession("stables", "farm");
        beginProduction(session);
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 0)).accepted());
        finishRound(session);
        for (String id : List.of("bot-one", "bot-two", "bot-three")) assertTrue(session.execute(new EndTurnCommand(id)).accepted());
        int before = session.snapshot().goldBalances().get("human");
        var purchase = session.execute(new BuyMarketCardCommand("human", 0, CardRole.BASE, -1));
        assertTrue(purchase.accepted());
        assertEquals(before - 3, purchase.snapshot().goldBalances().get("human"));
    }

    @Test
    void observatoryGrantsFreeNextMarketRefresh() {
        var session = marketSession("observatory", "farm");
        beginProduction(session);
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 0)).accepted());
        finishRound(session);
        for (String id : List.of("bot-one", "bot-two", "bot-three")) assertTrue(session.execute(new EndTurnCommand(id)).accepted());
        int before = session.snapshot().goldBalances().get("human");
        var refresh = session.execute(new RefreshMarketCommand("human"));
        assertTrue(refresh.accepted());
        assertEquals(before, refresh.snapshot().goldBalances().get("human"));
        assertFalse(session.execute(new RefreshMarketCommand("human")).accepted());
    }

    @Test
    void saleBonusAppliesWhenSourceIsSoldNextRound() {
        var session = marketSession("tax-collector", "treasurer");
        beginProduction(session);
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 0)).accepted());
        finishRound(session);
        assertTrue(session.execute(new EndTurnCommand("bot-one")).accepted());
        assertTrue(session.execute(new EndTurnCommand("bot-two")).accepted());
        assertTrue(session.execute(new EndTurnCommand("bot-three")).accepted());
        var before = session.snapshot().goldBalances().get("human");
        var sale = session.execute(new SellBasePackageCommand("human", 0));
        assertTrue(sale.accepted());
        assertEquals(before + 3, sale.snapshot().goldBalances().get("human"));
    }

    private static GameSession marketSession(String baseId, String marketId) {
        return marketSession(baseId, marketId, 1);
    }

    private static GameSession marketSession(String baseId, String marketId, int baseCount) {
        return marketSession(baseId, marketId, baseCount, 10);
    }

    private static GameSession marketSession(String baseId, String marketId, int baseCount, int requiredCards) {
        var input = GameSessionStageMarketEffectsTest.class.getResourceAsStream("/data/card-catalog.json");
        var catalog = new com.myjavacardgame.application.CardCatalogLoader().load(
                new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).catalog();
        var ids = List.of("human", "bot-one", "bot-two", "bot-three");
        var party = StartingPartyDefinition.fromSpecifications(ids.stream().map(id -> new StartingPartyParticipantSpecification(id,
                id.equals("human") ? ParticipantType.HUMAN : ParticipantType.BOT, 10, List.of("treasurer"))).toList(), catalog);
        var rules = new com.myjavacardgame.domain.board.BoardRules(6, 3);
        var board = BoardSnapshot.empty();
        for (int index = 0; index < baseCount; index++) {
            board = com.myjavacardgame.domain.board.BoardPlacement.placeBase(board,
                    CardInstance.fromCatalog("base-" + index, baseId, catalog), rules);
        }
        var boards = Map.of("human", board, "bot-one", BoardSnapshot.empty(), "bot-two", BoardSnapshot.empty(), "bot-three", BoardSnapshot.empty());
        var req = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 100, ResourceType.PROVISIONS, 100,
                ResourceType.BUILDING_MATERIALS, 100, ResourceType.SCROLLS, 100), requiredCards);
        var project = new ProjectDefinition("p", "Project", List.of(new ProjectStageDefinition("s", "Stage", req, "None")));
        var market = new com.myjavacardgame.domain.market.MarketDeck(new com.myjavacardgame.domain.market.MarketRules(1, 2, rules, 2),
                List.of(CardInstance.fromCatalog("market", marketId, catalog), CardInstance.fromCatalog("draw", marketId, catalog)), bound -> 0);
        return new GameSession(new GameSessionDefinition(party, 3, new DepositRules(3, 5), project, boards, market, Map.of()));
    }

    private static void beginProduction(GameSession session) {
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)).accepted());
        for (String id : List.of("human", "bot-one", "bot-two", "bot-three")) assertTrue(session.execute(new EndTurnCommand(id)).accepted());
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_MARKET)).accepted());
    }

    private static void finishRound(GameSession session) {
        for (String id : List.of("human", "bot-one", "bot-two", "bot-three")) assertTrue(session.execute(new EndTurnCommand(id)).accepted());
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_PRODUCTION)).accepted());
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_DEPOSIT)).accepted());
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)).accepted());
    }

    @Test
    void stageEffectsRejectNegativeAmounts() {
        assertThrows(IllegalArgumentException.class,
                () -> new StageEffect(StageEffect.Kind.CREDIT_EACH, ResourceType.GOLD, -1));
    }

    @Test
    void stageModifierCanBeNegative() {
        var effect = new StageEffect(StageEffect.Kind.MODIFY_PRODUCTION, ResourceType.BUILDING_MATERIALS, -1);
        assertEquals(-1, effect.amount());
    }
}
