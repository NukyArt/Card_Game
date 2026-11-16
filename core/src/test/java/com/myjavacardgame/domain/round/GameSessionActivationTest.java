package com.myjavacardgame.domain.round;

import static org.junit.jupiter.api.Assertions.*;

import com.myjavacardgame.domain.cards.CardProduction;
import com.myjavacardgame.domain.cards.ProductionEffect;
import com.myjavacardgame.domain.cards.CardCatalog;
import com.myjavacardgame.domain.cards.CardDefinition;
import com.myjavacardgame.domain.cards.CardInstance;
import com.myjavacardgame.domain.cards.CardRole;
import com.myjavacardgame.domain.cards.CardType;
import com.myjavacardgame.domain.board.BoardPlacement;
import com.myjavacardgame.domain.board.BoardRules;
import com.myjavacardgame.domain.board.BoardSnapshot;
import com.myjavacardgame.domain.party.ParticipantType;
import com.myjavacardgame.domain.party.StartingPartyDefinition;
import com.myjavacardgame.domain.party.StartingPartyParticipantSpecification;
import com.myjavacardgame.domain.project.ProjectDefinition;
import com.myjavacardgame.domain.project.ProjectStageDefinition;
import com.myjavacardgame.domain.project.ProjectStageRequirements;
import com.myjavacardgame.domain.cards.CardRoleParameters;
import com.myjavacardgame.domain.resources.ResourceType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameSessionActivationTest {
    private static final int PRODUCTION = 4;

    private static CardProduction catalogProduction(String id) {
        var stream = GameSessionActivationTest.class.getResourceAsStream("/data/card-catalog.json");
        assertNotNull(stream);
        return new com.myjavacardgame.application.CardCatalogLoader().load(
                new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))
                .catalog().requireCard(id).roleParameters().get(CardRole.BASE).production();
    }

    @Test
    void everyRealCatalogBaseActivatesWithValidChoices() {
        var stream = getClass().getResourceAsStream("/data/card-catalog.json");
        assertNotNull(stream);
        var catalog = new com.myjavacardgame.application.CardCatalogLoader().load(
                new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).catalog();
        assertEquals(20, catalog.definitionsById().size());
        for (String id : catalog.definitionsById().keySet()) {
            var session = realCatalogSession(catalog, id);
            List<ResourceType> choices = switch (id) {
                case "moneylender" -> List.of(ResourceType.SCROLLS);
                case "coal-mine" -> List.of(ResourceType.BUILDING_MATERIALS);
                default -> List.of();
            };
            List<Integer> goldChoices = switch (id) {
                case "winery", "forge" -> List.of(0);
                default -> List.of();
            };
            List<Boolean> exchanges = switch (id) {
                case "hunting-lodge", "laboratory" -> List.of(false);
                default -> List.of();
            };
            var command = new ActivateBaseCommand("human", 0, 0, choices, goldChoices, exchanges,
                    java.util.Optional.empty(), id.equals("citadel") ? java.util.Optional.of(bound -> 1) : java.util.Optional.empty());
            var result = session.execute(command);
            assertTrue(result.accepted(), () -> id + ": " + result.rejectionReason());
            assertEquals(id, result.snapshot().boards().get("human").slots().get(0).base().instance().definition().id());
            assertEquals(1, result.snapshot().boards().get("human").slots().get(0).lastActivatedRound().orElseThrow());
            var expected = expectedCatalogPayout(id);
            var progress = result.snapshot().projectProgress().orElseThrow();
            for (ResourceType resource : ResourceType.values()) {
                if (resource == ResourceType.GOLD) {
                    assertEquals(10 + expected.getOrDefault(resource, 0), result.snapshot().goldBalances().get("human"), id);
                } else {
                    assertEquals(expected.getOrDefault(resource, 0), progress.contributedResources().get(resource), id + ": " + resource);
                }
            }
        }
    }

    @Test
    void interactiveCitadelKeepsBothRandomGoldDrawsWhenStageGoldIsFull() {
        var stream = getClass().getResourceAsStream("/data/card-catalog.json");
        var catalog = new com.myjavacardgame.application.CardCatalogLoader().load(
                new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).catalog();
        var session = realCatalogSession(catalog, "citadel", 0);
        var interaction = session.beginActivation("human", 0,
                new com.myjavacardgame.domain.random.TransactionalRandomSource(bound -> 0));
        var first = assertInstanceOf(ActivationPrompt.RandomGoldAllocation.class,
                assertInstanceOf(ActivationInteraction.Prompt.class, interaction).prompt());
        interaction = session.answerActivation("human", new ActivationAnswer.Gold(first.activationId(), 0));
        var second = assertInstanceOf(ActivationPrompt.RandomGoldAllocation.class,
                assertInstanceOf(ActivationInteraction.Prompt.class, interaction).prompt());
        assertEquals(1, second.payout());
        interaction = session.answerActivation("human", new ActivationAnswer.Gold(second.activationId(), 0));
        assertInstanceOf(ActivationInteraction.Completed.class, interaction);
        assertEquals(12, session.snapshot().goldBalances().get("human"));
        assertEquals(0, first.maximumProjectGold());
        assertEquals(0, second.maximumProjectGold());
        assertEquals(5, session.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.SCROLLS));
    }

    private static Map<ResourceType, Integer> expectedCatalogPayout(String id) {
        return switch (id) {
            case "treasurer" -> Map.of(ResourceType.GOLD, 4);
            case "moneylender" -> Map.of(ResourceType.GOLD, 2, ResourceType.SCROLLS, 1);
            case "tax-collector" -> Map.of(ResourceType.GOLD, 3);
            case "inspector" -> Map.of(ResourceType.GOLD, 2);
            case "counterfeiter" -> Map.of(ResourceType.GOLD, 5, ResourceType.SCROLLS, 2);
            case "farm", "hunting-lodge" -> Map.of(ResourceType.PROVISIONS, 4);
            case "sheep" -> Map.of(ResourceType.PROVISIONS, 1);
            case "winery" -> Map.of(ResourceType.PROVISIONS, 3, ResourceType.GOLD, 1);
            case "stables" -> Map.of(ResourceType.PROVISIONS, 2);
            case "sawmill", "quarry" -> Map.of(ResourceType.BUILDING_MATERIALS, 4);
            case "coal-mine" -> Map.of(ResourceType.BUILDING_MATERIALS, 2);
            case "timber-storehouses" -> Map.of(ResourceType.BUILDING_MATERIALS, 1, ResourceType.SCROLLS, 1);
            case "forge" -> Map.of(ResourceType.BUILDING_MATERIALS, 3, ResourceType.SCROLLS, 1, ResourceType.GOLD, 1);
            case "apprentice-tower" -> Map.of(ResourceType.SCROLLS, 4);
            case "observatory" -> Map.of(ResourceType.SCROLLS, 3);
            case "laboratory" -> Map.of(ResourceType.SCROLLS, 2);
            case "crystal-ball" -> Map.of(ResourceType.SCROLLS, 1);
            case "citadel" -> Map.of(ResourceType.SCROLLS, 5, ResourceType.PROVISIONS, 2);
            default -> throw new IllegalArgumentException("Unexpected card: " + id);
        };
    }

    private static GameSession realCatalogSession(CardCatalog catalog, String baseId) {
        return realCatalogSession(catalog, baseId, 100);
    }

    private static GameSession realCatalogSession(CardCatalog catalog, String baseId, int goldRequired) {
        var ids = List.of("human", "bot-one", "bot-two", "bot-three");
        var party = StartingPartyDefinition.fromSpecifications(ids.stream().map(id ->
                new StartingPartyParticipantSpecification(id, id.equals("human") ? ParticipantType.HUMAN : ParticipantType.BOT,
                        10, List.of("treasurer"))).toList(), catalog);
        var rules = new BoardRules(6, 3);
        var board = BoardPlacement.placeBase(BoardSnapshot.empty(), CardInstance.fromCatalog("base", baseId, catalog), rules);
        var boards = Map.of("human", board, "bot-one", BoardSnapshot.empty(), "bot-two", BoardSnapshot.empty(), "bot-three", BoardSnapshot.empty());
        var requirements = new ProjectStageRequirements(Map.of(ResourceType.GOLD, goldRequired, ResourceType.PROVISIONS, 100,
                ResourceType.BUILDING_MATERIALS, 100, ResourceType.SCROLLS, 100), 0);
        var project = new ProjectDefinition("project", "Project", List.of(new ProjectStageDefinition("stage", "Stage", requirements, "None")));
        var session = new GameSession(new GameSessionDefinition(party, 2, new DepositRules(3, 5), project, boards),
                new com.myjavacardgame.domain.effects.DefaultEffectResolver(), rules);
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)).accepted());
        for (String id : ids) assertTrue(session.execute(new EndTurnCommand(id)).accepted());
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_MARKET)).accepted());
        return session;
    }

    @Test
    void stagePenaltyDoesNotReduceCardExchangeEffect() {
        var requirements = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 10, ResourceType.PROVISIONS, 10,
                ResourceType.BUILDING_MATERIALS, 10, ResourceType.SCROLLS, 10), 0);
        var progress = new com.myjavacardgame.domain.project.ProjectProgress(new ProjectDefinition("p", "Project", List.of(
                new ProjectStageDefinition("s", "Stage", requirements, "None"))));
        var production = catalogProduction("hunting-lodge");
        var definition = new CardDefinition("base", "Base", new CardType("type"),
                Map.of(CardRole.BASE, new CardRoleParameters(Map.of(), production)), "None");
        var catalog = new CardCatalog(Map.of("base", definition));
        var board = BoardPlacement.placeBase(BoardSnapshot.empty(), CardInstance.fromCatalog("card", "base", catalog), new BoardRules(6, 3));
        var penalty = new com.myjavacardgame.domain.project.StageEffect(
                com.myjavacardgame.domain.project.StageEffect.Kind.MODIFY_PRODUCTION, ResourceType.BUILDING_MATERIALS, -1);
        var stage = new StageResolution(progress, Map.of("human", 10), List.of(penalty));
        CardActivationResolver.resolve(board, board.slots().get(0), stage, "human", 0,
                List.of(ResourceType.BUILDING_MATERIALS), List.of(), List.of(true), java.util.Optional.empty());
        assertEquals(3, stage.progress.snapshot().contributedResources().get(ResourceType.BUILDING_MATERIALS));
        assertEquals(8, stage.balances.get("human"));
    }

    @Test
    void acceptsWholePackageWhenOnlyOneCardIsNeeded() {
        var requirements = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 10, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 1, ResourceType.SCROLLS, 10), 1);
        var session = productionSession(new CardProduction(Map.of(ResourceType.SCROLLS, 1), List.of()), true, List.of(requirements));
        var result = session.execute(new ContributeBoardPackageCommand("human", 0));
        assertTrue(result.accepted());
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedCardCount());
        assertEquals(1, result.snapshot().boards().get("human").slots().size());
        assertTrue(result.snapshot().boards().get("human").slots().get(0).upgrades().isEmpty());
    }

    @Test
    void loadedForgeResolvesScrollBeforeGoldAcrossStages() {
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 0, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 3, ResourceType.SCROLLS, 1), 0);
        var second = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 2, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var session = productionSession(catalogProduction("forge"), false, List.of(first, second));
        var result = session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(1)));
        assertTrue(result.accepted());
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().completedStageCount());
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.GOLD));
    }

    @Test
    void loadedLaboratoryCanExchangeForGold() {
        var session = productionSession(catalogProduction("laboratory"));
        var command = new ActivateBaseCommand("human", 0, 0, List.of(ResourceType.GOLD), List.of(1), List.of(true));
        var result = session.execute(command);
        assertTrue(result.accepted());
        assertEquals(8, result.snapshot().goldBalances().get("human"));
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.GOLD));
    }

    @Test
    void stageCreditCanFundExchangeAfterBasePayout() {
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 0, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var second = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 10, ResourceType.PROVISIONS, 10,
                ResourceType.BUILDING_MATERIALS, 10, ResourceType.SCROLLS, 10), 0);
        var firstStage = new ProjectStageDefinition("first", "First", first, "Credit", List.of(), List.of(
                new com.myjavacardgame.domain.project.StageEffect(com.myjavacardgame.domain.project.StageEffect.Kind.CREDIT_EACH, ResourceType.GOLD, 2)));
        var progress = new com.myjavacardgame.domain.project.ProjectProgress(new ProjectDefinition("p", "Project", List.of(
                firstStage, new ProjectStageDefinition("second", "Second", second, "None"))));
        var production = new CardProduction(Map.of(ResourceType.PROVISIONS, 1), List.of(
                new ProductionEffect(ProductionEffect.Kind.EXCHANGE, 3, 2, List.of(ResourceType.BUILDING_MATERIALS))));
        var role = new CardRoleParameters(Map.of(), production);
        var definition = new CardDefinition("base", "Base", new CardType("type"), Map.of(CardRole.BASE, role), "None");
        var catalog = new CardCatalog(Map.of("base", definition));
        var board = BoardPlacement.placeBase(BoardSnapshot.empty(), CardInstance.fromCatalog("card", "base", catalog), new BoardRules(6, 3));
        var resolution = new StageResolution(progress, Map.of("human", 0, "bot", 0), List.of());
        CardActivationResolver.resolve(board, board.slots().get(0), resolution, "human", 0,
                List.of(ResourceType.BUILDING_MATERIALS), List.of(), List.of(true), java.util.Optional.empty());
        assertEquals(0, resolution.balances.get("human"));
        assertEquals(2, resolution.balances.get("bot"));
        assertEquals(3, resolution.progress.snapshot().contributedResources().get(ResourceType.BUILDING_MATERIALS));
    }

    @Test
    void randomActivationResolvesStageCreditBeforeNextDraw() {
        var effect = new ProductionEffect(ProductionEffect.Kind.RANDOM, 2, 0, List.of(
                ResourceType.GOLD, ResourceType.PROVISIONS, ResourceType.BUILDING_MATERIALS, ResourceType.SCROLLS));
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 0, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var second = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 10, ResourceType.PROVISIONS, 10,
                ResourceType.BUILDING_MATERIALS, 10, ResourceType.SCROLLS, 10), 0);
        var production = new CardProduction(Map.of(ResourceType.SCROLLS, 1), List.of(effect));
        var stage = new StageResolution(new com.myjavacardgame.domain.project.ProjectProgress(new ProjectDefinition("p", "Project", List.of(
                new ProjectStageDefinition("first", "First", first, "Credit", List.of(), List.of(
                        new com.myjavacardgame.domain.project.StageEffect(com.myjavacardgame.domain.project.StageEffect.Kind.CREDIT_EACH, ResourceType.GOLD, 2))),
                new ProjectStageDefinition("second", "Second", second, "None")))), Map.of("human", 10, "bot", 10), List.of());
        var role = new CardRoleParameters(Map.of(), production);
        var definition = new CardDefinition("base", "Base", new CardType("type"), Map.of(CardRole.BASE, role), "None");
        var catalog = new CardCatalog(Map.of("base", definition));
        var board = BoardPlacement.placeBase(BoardSnapshot.empty(), CardInstance.fromCatalog("card", "base", catalog), new BoardRules(6, 3));
        int[] sequence = {1, 0};
        int[] index = {0};
        var pending = CardActivationResolver.resolve(board, board.slots().get(0), stage, "human", 0, List.of(), List.of(), List.of(),
                java.util.Optional.of(bound -> sequence[index[0]++]));
        assertTrue(pending.pending().isPresent());
        assertEquals(2, index[0]);
        assertEquals(12, stage.balances.get("human"));
        assertEquals(12, stage.balances.get("bot"));
        assertEquals(1, stage.progress.snapshot().completedStageCount());
    }

    @Test
    void productionModifiersApplyInInsertionOrderAfterUpgrade() {
        var production = new CardProduction(Map.of(ResourceType.SCROLLS, 1), List.of());
        var requirements = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 10, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 1, ResourceType.SCROLLS, 10), 0);
        var stage = new ProjectStageDefinition("stage", "Stage", requirements, "None");
        var progress = new com.myjavacardgame.domain.project.ProjectProgress(new ProjectDefinition("project", "Project", List.of(stage)));
        var modifiers = List.of(new com.myjavacardgame.domain.project.StageEffect(
                com.myjavacardgame.domain.project.StageEffect.Kind.MODIFY_PRODUCTION, ResourceType.SCROLLS, -9),
                new com.myjavacardgame.domain.project.StageEffect(
                com.myjavacardgame.domain.project.StageEffect.Kind.MODIFY_PRODUCTION, ResourceType.SCROLLS, 1));
        var role = new CardRoleParameters(Map.of(), production);
        var def = new CardDefinition("base", "Base", new CardType("type"), Map.of(CardRole.BASE, role), "None");
        var catalog = new CardCatalog(Map.of("base", def));
        var board = BoardPlacement.placeBase(BoardSnapshot.empty(), CardInstance.fromCatalog("card", "base", catalog), new BoardRules(6, 3));
        var resolution = new StageResolution(progress, Map.of("human", 10), modifiers);
        CardActivationResolver.resolve(board, board.slots().get(0), resolution, "human", 0, List.of(), List.of(), List.of(), java.util.Optional.empty());
        assertEquals(1, resolution.progress.snapshot().contributedResources().get(ResourceType.SCROLLS));
    }

    @Test
    void matchingQuarryPurchaseProducesMaterials() {
        var stream = getClass().getResourceAsStream("/data/card-catalog.json");
        var catalog = new com.myjavacardgame.application.CardCatalogLoader().load(
                new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).catalog();
        var specs = List.of(
                new StartingPartyParticipantSpecification("human", ParticipantType.HUMAN, 10, List.of("treasurer")),
                new StartingPartyParticipantSpecification("bot-one", ParticipantType.BOT, 10, List.of("treasurer")),
                new StartingPartyParticipantSpecification("bot-two", ParticipantType.BOT, 10, List.of("treasurer")),
                new StartingPartyParticipantSpecification("bot-three", ParticipantType.BOT, 10, List.of("treasurer")));
        var party = StartingPartyDefinition.fromSpecifications(specs, catalog);
        var boardRules = new BoardRules(6, 3);
        var board = BoardPlacement.placeBase(BoardSnapshot.empty(), CardInstance.fromCatalog("quarry-one", "quarry", catalog), boardRules);
        var boards = Map.of("human", board, "bot-one", BoardSnapshot.empty(), "bot-two", BoardSnapshot.empty(), "bot-three", BoardSnapshot.empty());
        var stage = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 10, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 1, ResourceType.SCROLLS, 1), 0);
        var project = new ProjectDefinition("project", "Project", List.of(new ProjectStageDefinition("stage", "Stage", stage, "None")));
        var marketRules = new com.myjavacardgame.domain.market.MarketRules(1, 2, boardRules, 2);
        var market = new com.myjavacardgame.domain.market.MarketDeck(marketRules,
                List.of(CardInstance.fromCatalog("market-mine", "coal-mine", catalog)), bound -> 0);
        var session = new GameSession(new GameSessionDefinition(party, 2, new DepositRules(3, 5), project, boards, market, Map.of()));
        assertTrue(session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME)).accepted());
        var purchase = session.execute(new BuyMarketCardCommand("human", 0, CardRole.BASE, -1));
        assertTrue(purchase.accepted());
        assertEquals(1, purchase.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.BUILDING_MATERIALS));
    }

    @Test
    void loadedMineKeepsUpgradeWhenChoosingGold() {
        var result = productionSession(catalogProduction("coal-mine"), true)
                .execute(new ActivateBaseCommand("human", 0, 1, List.of(ResourceType.GOLD)));
        assertTrue(result.accepted());
        assertEquals(11, result.snapshot().goldBalances().get("human"));
        assertEquals(3, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.SCROLLS));
    }

    @Test
    void loadedHuntingLodgeExchangesGoldForMaterials() {
        var command = new ActivateBaseCommand("human", 0, 0, List.of(ResourceType.BUILDING_MATERIALS), List.of(), List.of(true));
        var result = productionSession(catalogProduction("hunting-lodge")).execute(command);
        assertTrue(result.accepted());
        assertEquals(8, result.snapshot().goldBalances().get("human"));
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.BUILDING_MATERIALS));
    }

    @Test
    void loadedCitadelProducesTwoIndependentResources() {
        int[] draws = {1, 2};
        int[] index = {0};
        var command = new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(),
                java.util.Optional.empty(), java.util.Optional.of(bound -> draws[index[0]++]));
        var result = productionSession(catalogProduction("citadel")).execute(command);
        assertTrue(result.accepted());
        assertEquals(2, index[0]);
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.PROVISIONS));
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.BUILDING_MATERIALS));
    }

    @Test
    void loadedInspectorTransfersPackage() {
        var transfer = new ActivateBaseCommand.Transfer("human", 0, "bot-one");
        var command = new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(), java.util.Optional.of(transfer));
        var result = productionSession(catalogProduction("inspector"), true).execute(command);
        assertTrue(result.accepted());
        var moved = result.snapshot().boards().get("bot-one").slots().get(0);
        assertEquals(1, moved.upgrades().size());
        assertEquals(1, moved.lastActivatedRound().orElseThrow());
    }

    @Test
    void sequentialPayoutTargetsNextStage() {
        var effect = new ProductionEffect(ProductionEffect.Kind.FIXED, 2, 0, List.of(ResourceType.SCROLLS));
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var second = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 0, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 2), 0);
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)), false,
                List.of(first, second));
        var result = session.execute(new ActivateBaseCommand("human", 0, 1));
        assertTrue(result.accepted());
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().completedStageCount());
        assertEquals(2, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.SCROLLS));
    }

    @Test
    void victoryDoesNotRequireAdditionalGoldAllocation() {
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var effect = new ProductionEffect(ProductionEffect.Kind.FIXED, 1, 0, List.of(ResourceType.GOLD));
        var session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)), false, List.of(first));
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 1)).accepted());
    }

    @Test
    void victoryDoesNotRequireRandomSource() {
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var effect = new ProductionEffect(ProductionEffect.Kind.RANDOM, 2, 0, List.of(
                ResourceType.GOLD, ResourceType.PROVISIONS, ResourceType.BUILDING_MATERIALS, ResourceType.SCROLLS));
        var session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)), false, List.of(first));
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 1)).accepted());
    }

    @Test
    void victoryDoesNotRequireExchangeDecision() {
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var effect = new ProductionEffect(ProductionEffect.Kind.EXCHANGE, 1, 2, List.of(ResourceType.SCROLLS));
        var session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)), false, List.of(first));
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 1)).accepted());
    }

    @Test
    void victoryDoesNotRequireLaterChoice() {
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var effect = new ProductionEffect(ProductionEffect.Kind.CHOICE, 1, 0, List.of(ResourceType.SCROLLS));
        var session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)), false, List.of(first));
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 1)).accepted());
        assertEquals(MatchOutcome.VICTORY, session.snapshot().outcome());
    }

    @Test
    void victoryStopsLaterExchange() {
        var effect = new ProductionEffect(ProductionEffect.Kind.EXCHANGE, 1, 15, List.of(ResourceType.SCROLLS));
        var finalStage = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)), false,
                List.of(finalStage));
        var result = session.execute(new ActivateBaseCommand("human", 0, 1, List.of(ResourceType.SCROLLS), List.of(), List.of(true)));
        assertTrue(result.accepted());
        assertEquals(MatchOutcome.VICTORY, result.snapshot().outcome());
        assertEquals(13, result.snapshot().goldBalances().get("human"));
    }

    @Test
    void loadedWineryProducesAdditionalGold() {
        var stream = getClass().getResourceAsStream("/data/card-catalog.json");
        assertNotNull(stream);
        var catalog = new com.myjavacardgame.application.CardCatalogLoader().load(
                new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8));
        var production = catalog.catalog().requireCard("winery").roleParameters().get(CardRole.BASE).production();
        var result = productionSession(production).execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(0)));
        assertTrue(result.accepted());
        assertEquals(11, result.snapshot().goldBalances().get("human"));
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.PROVISIONS));
    }

    @Test
    void randomGoldFinishingProjectPreventsSecondDraw() {
        var requirement = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var effect = new ProductionEffect(ProductionEffect.Kind.RANDOM, 2, 0, List.of(
                ResourceType.GOLD, ResourceType.PROVISIONS, ResourceType.BUILDING_MATERIALS, ResourceType.SCROLLS));
        var session = productionSession(new CardProduction(Map.of(ResourceType.SCROLLS, 1), List.of(effect)), false, List.of(requirement));
        int[] draws = {0};
        var start = session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(),
                java.util.Optional.empty(), java.util.Optional.of(bound -> { draws[0]++; return 0; })));
        assertTrue(start.accepted());
        var result = session.execute(new ResolveRandomGoldCommand("human", 1));
        assertTrue(result.accepted());
        assertEquals(1, draws[0]);
        assertEquals(MatchOutcome.VICTORY, result.snapshot().outcome());
        assertTrue(result.snapshot().pendingRandomGold().isEmpty());
    }

    @Test
    void victoryFromRandomPayoutStopsLaterDraws() {
        var requirement = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 0, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var effect = new ProductionEffect(ProductionEffect.Kind.RANDOM, 2, 0, List.of(
                ResourceType.GOLD, ResourceType.PROVISIONS, ResourceType.BUILDING_MATERIALS, ResourceType.SCROLLS));
        var session = productionSession(new CardProduction(Map.of(ResourceType.SCROLLS, 1), List.of(effect)), false, List.of(requirement));
        int[] draws = {0};
        var result = session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(),
                java.util.Optional.empty(), java.util.Optional.of(bound -> { draws[0]++; return 1; })));
        assertTrue(result.accepted());
        assertEquals(1, draws[0]);
        assertEquals(MatchOutcome.VICTORY, result.snapshot().outcome());
        assertTrue(result.snapshot().pendingRandomGold().isEmpty());
    }

    @Test
    void randomGoldChoiceThenResourceCompletesStageOnce() {
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 1, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var second = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 5, ResourceType.PROVISIONS, 5,
                ResourceType.BUILDING_MATERIALS, 5, ResourceType.SCROLLS, 5), 0);
        var session = productionSession(catalogProduction("citadel"), false, List.of(first, second));
        int[] sequence = {0, 1};
        int[] index = {0};
        var start = session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(),
                java.util.Optional.empty(), java.util.Optional.of(bound -> sequence[index[0]++])));
        assertTrue(start.accepted());
        assertEquals(1, index[0]);
        assertTrue(start.events().isEmpty());
        var before = session.snapshot();
        assertFalse(session.execute(new ResolveRandomGoldCommand("human", 2)).accepted());
        assertEquals(before, session.snapshot());
        assertEquals(1, index[0]);
        var result = session.execute(new ResolveRandomGoldCommand("human", 1));
        assertTrue(result.accepted());
        assertEquals(2, index[0]);
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().completedStageCount());
        assertEquals(1, result.events().size());
        assertInstanceOf(com.myjavacardgame.domain.project.ProjectStageCompletedEvent.class, result.events().get(0));
        assertTrue(result.snapshot().pendingRandomGold().isEmpty());
        assertFalse(session.execute(new ResolveRandomGoldCommand("human", 1)).accepted());
        assertEquals(2, index[0]);
    }

    @Test
    void pendingRandomGoldRejectsOtherCommandsAndInvalidAllocations() {
        int[] draws = {0};
        GameSession session = productionSession(catalogProduction("citadel"));
        var start = session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(),
                java.util.Optional.empty(), java.util.Optional.of(bound -> { draws[0]++; return 0; })));
        assertTrue(start.accepted());
        assertEquals(1, draws[0]);
        var before = session.snapshot();
        assertFalse(session.execute(new ResolveRandomGoldCommand("human", 2)).accepted());
        assertFalse(session.execute(new ResolveRandomGoldCommand("bot-one", 1)).accepted());
        assertFalse(session.execute(new EndTurnCommand("human")).accepted());
        assertEquals(before, session.snapshot());
        assertEquals(1, draws[0]);
        var first = session.execute(new ResolveRandomGoldCommand("human", 1));
        assertTrue(first.accepted());
        assertEquals(2, draws[0]);
        assertTrue(first.snapshot().pendingRandomGold().isPresent());
        var second = session.execute(new ResolveRandomGoldCommand("human", 0));
        assertTrue(second.accepted());
        assertTrue(second.snapshot().pendingRandomGold().isEmpty());
        assertEquals(2, draws[0]);
        assertEquals(11, second.snapshot().goldBalances().get("human"));
    }

    @Test
    void randomGoldChoiceReceivesNewStageAfterEarlierPayout() {
        var first = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 0, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var second = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 3, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var effect = new ProductionEffect(ProductionEffect.Kind.RANDOM, 2, 0, List.of(
                ResourceType.GOLD, ResourceType.PROVISIONS, ResourceType.BUILDING_MATERIALS, ResourceType.SCROLLS));
        var session = productionSession(new CardProduction(Map.of(ResourceType.SCROLLS, 1), List.of(effect)), false, List.of(first, second));
        int[] draws = {1, 0};
        int[] index = {0};
        var command = new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(),
                java.util.Optional.empty(), java.util.Optional.of(bound -> draws[index[0]++]));
        var result = session.execute(command);
        assertTrue(result.accepted());
        assertEquals(2, index[0]);
        assertEquals("stage-1", result.snapshot().projectProgress().orElseThrow().currentStageId().orElseThrow());
        assertEquals(3, result.snapshot().pendingRandomGold().orElseThrow().remainingStageGold());
        assertTrue(result.snapshot().pendingRandomGold().isPresent());
        var chosen = session.execute(new ResolveRandomGoldCommand("human", 1));
        assertTrue(chosen.accepted());
        assertTrue(chosen.snapshot().pendingRandomGold().isEmpty());
        assertEquals(1, chosen.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.GOLD));
    }

    @Test
    void randomGoldHonorsAllocationAndExcess() {
        var effect = new ProductionEffect(ProductionEffect.Kind.RANDOM, 2, 0, List.of(
                ResourceType.GOLD, ResourceType.PROVISIONS, ResourceType.BUILDING_MATERIALS, ResourceType.SCROLLS));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        var command = new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(),
                java.util.Optional.empty(), java.util.Optional.of(bound -> 0));
        assertTrue(session.execute(command).accepted());
        assertTrue(session.execute(new ResolveRandomGoldCommand("human", 1)).accepted());
        var result = session.execute(new ResolveRandomGoldCommand("human", 0));
        assertTrue(result.accepted());
        assertEquals(15, result.snapshot().goldBalances().get("human"));
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.GOLD));
    }

    @Test
    void invalidRandomAllocationDoesNotConsumeDraws() {
        var effect = new ProductionEffect(ProductionEffect.Kind.RANDOM, 2, 0, List.of(
                ResourceType.GOLD, ResourceType.PROVISIONS, ResourceType.BUILDING_MATERIALS, ResourceType.SCROLLS));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        int[] calls = {0};
        var command = new ActivateBaseCommand("human", 0, 0, List.of(), List.of(2, 0), List.of(),
                java.util.Optional.empty(), java.util.Optional.of(bound -> { calls[0]++; return 0; }));
        var before = session.snapshot();
        assertFalse(session.execute(command).accepted());
        assertEquals(0, calls[0]);
        assertEquals(before, session.snapshot());
    }

    @Test
    void randomProductionUsesIndependentInjectedDraws() {
        var effect = new ProductionEffect(ProductionEffect.Kind.RANDOM, 2, 0, List.of(
                ResourceType.GOLD, ResourceType.PROVISIONS, ResourceType.BUILDING_MATERIALS, ResourceType.SCROLLS));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        int[] draws = {1, 3};
        int[] index = {0};
        var command = new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(),
                java.util.Optional.empty(), java.util.Optional.of(bound -> draws[index[0]++]));
        var result = session.execute(command);
        assertTrue(result.accepted());
        assertEquals(2, index[0]);
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.PROVISIONS));
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.SCROLLS));
    }

    @Test
    void invalidInspectorRecipientDoesNotActivateBase() {
        var effect = new ProductionEffect(ProductionEffect.Kind.TRANSFER, 0, 0, List.of());
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)), true);
        var before = session.snapshot();
        var transfer = new ActivateBaseCommand.Transfer("human", 0, "unknown");
        assertFalse(session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(), java.util.Optional.of(transfer))).accepted());
        assertEquals(before, session.snapshot());
    }

    @Test
    void inspectorRejectsFullRecipientWithoutChanges() {
        var effect = new ProductionEffect(ProductionEffect.Kind.TRANSFER, 0, 0, List.of());
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)), true,
                List.of(new ProjectStageRequirements(Map.of(ResourceType.GOLD, 10, ResourceType.PROVISIONS, 1,
                        ResourceType.BUILDING_MATERIALS, 1, ResourceType.SCROLLS, 10), 0)), new BoardRules(0, 3));
        var before = session.snapshot();
        var transfer = new ActivateBaseCommand.Transfer("human", 0, "bot-one");
        assertFalse(session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(), java.util.Optional.of(transfer))).accepted());
        assertEquals(before, session.snapshot());
    }

    @Test
    void inspectorTransfersActivatedBaseWithUpgrade() {
        var effect = new ProductionEffect(ProductionEffect.Kind.TRANSFER, 0, 0, List.of());
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)), true);
        var transfer = new ActivateBaseCommand.Transfer("human", 0, "bot-one");
        var result = session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(), java.util.Optional.of(transfer)));
        assertTrue(result.accepted());
        assertEquals(1, result.snapshot().boards().get("human").slots().size());
        var moved = result.snapshot().boards().get("bot-one").slots().get(0);
        assertEquals(1, moved.upgrades().size());
        assertEquals(1, moved.lastActivatedRound().orElseThrow());
    }

    @Test
    void insufficientExchangeGoldRejectsEntireActivation() {
        var effect = new ProductionEffect(ProductionEffect.Kind.EXCHANGE, 1, 15, List.of(ResourceType.SCROLLS));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        var before = session.snapshot();
        assertFalse(session.execute(new ActivateBaseCommand("human", 0, 0, List.of(ResourceType.SCROLLS), List.of(), List.of(true))).accepted());
        assertEquals(before, session.snapshot());
    }

    @Test
    void exchangeSpendsGoldAfterBaseProduction() {
        var effect = new ProductionEffect(ProductionEffect.Kind.EXCHANGE, 1, 12, List.of(ResourceType.SCROLLS));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        var result = session.execute(new ActivateBaseCommand("human", 0, 0, List.of(ResourceType.SCROLLS), List.of(), List.of(true)));
        assertTrue(result.accepted());
        assertEquals(2, result.snapshot().goldBalances().get("human"));
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.SCROLLS));
    }

    @Test
    void optionalExchangeCanBeDeclined() {
        var effect = new ProductionEffect(ProductionEffect.Kind.EXCHANGE, 1, 12, List.of(ResourceType.SCROLLS));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        var result = session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(), List.of(false)));
        assertTrue(result.accepted());
        assertEquals(14, result.snapshot().goldBalances().get("human"));
    }

    @Test
    void mineCanKeepMaterialsWhileUpgradeProduces() {
        var effect = new ProductionEffect(ProductionEffect.Kind.REPLACE_BASE, 2, 0,
                List.of(ResourceType.BUILDING_MATERIALS, ResourceType.GOLD));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.BUILDING_MATERIALS, 2), List.of(effect)), true);
        var result = session.execute(new ActivateBaseCommand("human", 0, 0, List.of(ResourceType.BUILDING_MATERIALS)));
        assertTrue(result.accepted());
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.BUILDING_MATERIALS));
        assertEquals(3, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.SCROLLS));
    }

    @Test
    void mineReplacementPreservesUpgradeProduction() {
        var effect = new ProductionEffect(ProductionEffect.Kind.REPLACE_BASE, 2, 0,
                List.of(ResourceType.BUILDING_MATERIALS, ResourceType.GOLD));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.BUILDING_MATERIALS, 2), List.of(effect)), true);
        var result = session.execute(new ActivateBaseCommand("human", 0, 1, List.of(ResourceType.GOLD)));
        assertTrue(result.accepted());
        assertEquals(11, result.snapshot().goldBalances().get("human"));
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.GOLD));
        assertEquals(3, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.SCROLLS));
        assertEquals(0, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.BUILDING_MATERIALS));
    }

    @Test
    void copyBonusCountsOtherBasesButNotUpgrades() {
        var effect = new ProductionEffect(ProductionEffect.Kind.PER_OTHER_BASE, 1, 0, List.of(ResourceType.SCROLLS));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.SCROLLS, 1), List.of(effect)), true);
        var result = session.execute(new ActivateBaseCommand("human", 0, 0));
        assertTrue(result.accepted());
        assertEquals(5, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.SCROLLS));
    }

    @Test
    void activationContributesGoldAndRejectsSecondUse() {
        GameSession session = productionSession();
        GameSessionCommandResult result = session.execute(new ActivateBaseCommand("human", 0, 2));
        assertTrue(result.accepted());
        assertEquals(12, result.snapshot().goldBalances().get("human"));
        assertEquals(2, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.GOLD));
        GameSessionSnapshot before = session.snapshot();
        assertFalse(session.execute(new ActivateBaseCommand("human", 0, 2)).accepted());
        assertEquals(before, session.snapshot());
    }

    @Test
    void invalidAllocationLeavesBaseAvailable() {
        GameSession session = productionSession();
        GameSessionSnapshot before = session.snapshot();
        assertFalse(session.execute(new ActivateBaseCommand("human", 0, 5)).accepted());
        assertEquals(before, session.snapshot());
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 2)).accepted());
    }

    @Test
    void excessAdditionalGoldRemainsOnBalance() {
        var effect = new ProductionEffect(ProductionEffect.Kind.FIXED, 2, 0, List.of(ResourceType.GOLD));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        assertTrue(session.execute(new ContributeGoldCommand("human", 9)).accepted());
        var result = session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(2)));
        assertTrue(result.accepted());
        assertEquals(6, result.snapshot().goldBalances().get("human"));
        assertEquals(10, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.GOLD));
    }

    @Test
    void invalidAdditionalGoldAllocationDoesNotActivateBase() {
        var effect = new ProductionEffect(ProductionEffect.Kind.FIXED, 2, 0, List.of(ResourceType.GOLD));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        var before = session.snapshot();
        assertFalse(session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(3))).accepted());
        assertEquals(before, session.snapshot());
        assertTrue(session.execute(new ActivateBaseCommand("human", 0, 0, List.of(), List.of(2))).accepted());
    }

    @Test
    void additionalGoldHasIndependentAllocation() {
        var effect = new ProductionEffect(ProductionEffect.Kind.FIXED, 2, 0, List.of(ResourceType.GOLD));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        var result = session.execute(new ActivateBaseCommand("human", 0, 2, List.of(), List.of(1)));
        assertTrue(result.accepted());
        assertEquals(13, result.snapshot().goldBalances().get("human"));
        assertEquals(3, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.GOLD));
    }

    @Test
    void chosenAdditionalResourceIsAppliedAfterBasePayout() {
        var effect = new ProductionEffect(ProductionEffect.Kind.CHOICE, 1, 0,
                List.of(ResourceType.PROVISIONS, ResourceType.SCROLLS));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        var result = session.execute(new ActivateBaseCommand("human", 0, 2, List.of(ResourceType.SCROLLS)));
        assertTrue(result.accepted());
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.SCROLLS));
    }

    @Test
    void invalidAdditionalResourceRejectsWithoutActivating() {
        var effect = new ProductionEffect(ProductionEffect.Kind.CHOICE, 1, 0, List.of(ResourceType.SCROLLS));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        var before = session.snapshot();
        assertFalse(session.execute(new ActivateBaseCommand("human", 0, 2, List.of(ResourceType.GOLD))).accepted());
        assertEquals(before, session.snapshot());
    }

    @Test
    void fixedAdditionalProductionIsAppliedAfterBasePayout() {
        var effect = new ProductionEffect(ProductionEffect.Kind.FIXED, 1, 0, List.of(ResourceType.SCROLLS));
        GameSession session = productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of(effect)));
        var result = session.execute(new ActivateBaseCommand("human", 0, 2));
        assertTrue(result.accepted());
        assertEquals(1, result.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.SCROLLS));
    }

    private static GameSession productionSession() {
        return productionSession(new CardProduction(Map.of(ResourceType.GOLD, 4), List.of()));
    }

    private static GameSession productionSession(CardProduction production) {
        return productionSession(production, false);
    }

    private static GameSession productionSession(CardProduction production, boolean copies) {
        var requirements = new ProjectStageRequirements(Map.of(ResourceType.GOLD, 10, ResourceType.PROVISIONS, 1,
                ResourceType.BUILDING_MATERIALS, 1, ResourceType.SCROLLS, copies ? 10 : 1), 0);
        return productionSession(production, copies, List.of(requirements));
    }

    private static GameSession productionSession(CardProduction production, boolean copies, List<ProjectStageRequirements> stages) {
        return productionSession(production, copies, stages, new BoardRules(6, 3));
    }

    private static GameSession productionSession(CardProduction production, boolean copies, List<ProjectStageRequirements> stages, BoardRules boardRules) {
        return productionSession(production, copies, stages, boardRules,
                new CardProduction(Map.of(ResourceType.SCROLLS, 3), List.of()));
    }

    private static GameSession productionSession(CardProduction production, boolean copies, List<ProjectStageRequirements> stages,
            BoardRules boardRules, CardProduction upgradeProduction) {
        var role = new CardRoleParameters(Map.of(), production);
        var upgrade = new CardRoleParameters(Map.of(), upgradeProduction);
        var definition = new CardDefinition("base", "Base", new CardType("type"), Map.of(CardRole.BASE, role, CardRole.UPGRADE, upgrade), "");
        var catalog = new CardCatalog(Map.of("base", definition));
        var specs = List.of(
                new StartingPartyParticipantSpecification("human", ParticipantType.HUMAN, 10, List.of("base")),
                new StartingPartyParticipantSpecification("bot-one", ParticipantType.BOT, 10, List.of("base")),
                new StartingPartyParticipantSpecification("bot-two", ParticipantType.BOT, 10, List.of("base")),
                new StartingPartyParticipantSpecification("bot-three", ParticipantType.BOT, 10, List.of("base")));
        var party = StartingPartyDefinition.fromSpecifications(specs, catalog);
        var board = BoardPlacement.placeBase(BoardSnapshot.empty(), CardInstance.fromCatalog("card", "base", catalog), new BoardRules(6, 3));
        if (copies) {
            board = BoardPlacement.placeBase(board, CardInstance.fromCatalog("other", "base", catalog), new BoardRules(6, 3));
            board = BoardPlacement.placeUpgrade(board, CardInstance.fromCatalog("upgrade", "base", catalog), 0, new BoardRules(6, 3));
        }
        var boards = Map.of("human", board, "bot-one", BoardSnapshot.empty(), "bot-two", BoardSnapshot.empty(), "bot-three", BoardSnapshot.empty());
        var project = new ProjectDefinition("project", "Project", java.util.stream.IntStream.range(0, stages.size())
                .mapToObj(index -> new ProjectStageDefinition("stage-" + index, "Stage", stages.get(index), "None"))
                .toList());
        var session = new GameSession(new GameSessionDefinition(party, 2, new DepositRules(3, 5), project, boards),
                new com.myjavacardgame.domain.effects.DefaultEffectResolver(), boardRules);
        session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_INCOME));
        for (String participant : List.of("human", "bot-one", "bot-two", "bot-three")) session.execute(new EndTurnCommand(participant));
        session.execute(new SessionPhaseCommand(PhaseCommand.COMPLETE_MARKET));
        return session;
    }

    @Test
    void interactiveGoldMaximumIncludesUpgradeAndIsCappedByRemainingStageDemand() {
        var baseProduction = catalogProduction("treasurer");
        var stream = getClass().getResourceAsStream("/data/card-catalog.json");
        var catalog = new com.myjavacardgame.application.CardCatalogLoader().load(
                new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).catalog();
        var upgradeProduction = catalog.requireCard("inspector").roleParameters().get(CardRole.UPGRADE).production();
        int payout = baseProduction.amounts().get(ResourceType.GOLD) + upgradeProduction.amounts().get(ResourceType.GOLD);
        for (int demand : List.of(payout - 1, payout + 1)) {
            var requirements = new ProjectStageRequirements(Map.of(ResourceType.GOLD, demand, ResourceType.PROVISIONS, 1,
                    ResourceType.BUILDING_MATERIALS, 1, ResourceType.SCROLLS, 1), 0);
            var session = productionSession(baseProduction, true, List.of(requirements), new BoardRules(6, 3), upgradeProduction);
            var before = session.snapshot();
            var prompt = assertInstanceOf(ActivationPrompt.GoldAllocation.class,
                    assertInstanceOf(ActivationInteraction.Prompt.class, session.beginActivation("human", 0,
                            new com.myjavacardgame.domain.random.TransactionalRandomSource(bound -> 0))).prompt());
            assertEquals(payout, prompt.payout());
            assertEquals(Math.min(payout, demand), prompt.maximumProjectGold());
            assertEquals(before, session.snapshot());
            assertInstanceOf(ActivationInteraction.Completed.class,
                    session.answerActivation("human", new ActivationAnswer.Gold(prompt.activationId(), prompt.maximumProjectGold())));
            assertEquals(Math.min(payout, demand), session.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.GOLD));
            assertEquals(before.goldBalances().get("human") + payout - Math.min(payout, demand), session.snapshot().goldBalances().get("human"));
        }
    }

    @Test
    void activationCommandRejectsNegativeGoldSplit() {
        assertThrows(IllegalArgumentException.class, () -> new ActivateBaseCommand("human", 0, -1));
    }

    @Test
    void productionDataIsImmutableAndAttachedToTheRole() {
        Map<ResourceType, Integer> amounts = new HashMap<>();
        amounts.put(ResourceType.PROVISIONS, PRODUCTION);
        CardProduction production = new CardProduction(amounts, List.of());
        CardRoleParameters role = new CardRoleParameters(Map.of(), production);
        amounts.clear();
        assertEquals(PRODUCTION, role.production().amounts().get(ResourceType.PROVISIONS));
        assertThrows(UnsupportedOperationException.class, () -> role.production().amounts().clear());
    }

    @Test
    void productionRejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException.class,
                () -> new CardProduction(Map.of(ResourceType.GOLD, -1), List.of()));
    }

    @Test
    void existingRoleParametersDefaultToEmptyProduction() {
        assertTrue(new CardRoleParameters(Map.of()).production().amounts().isEmpty());
    }
}
