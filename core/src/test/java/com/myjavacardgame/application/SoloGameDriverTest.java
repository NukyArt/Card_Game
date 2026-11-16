package com.myjavacardgame.application;

import com.myjavacardgame.domain.cards.*;
import com.myjavacardgame.domain.party.*;
import com.myjavacardgame.domain.project.*;
import com.myjavacardgame.domain.resources.ResourceType;
import com.myjavacardgame.domain.round.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SoloGameDriverTest {
    private static final int TURN_SECONDS = 30;
    private static final int STARTING_GOLD = 10;
    private static final List<String> IDS = List.of("human", "bot-1", "bot-2", "bot-3");

    private SoloGameDriver driver(int roundLimit) {
        var definition = new CardDefinition("base", "Base", new CardType("test"),
                Map.of(CardRole.BASE, new CardRoleParameters(Map.of())), "");
        var catalog = new CardCatalog(Map.of("base", definition));
        var party = StartingPartyDefinition.fromSpecifications(IDS.stream().map(id ->
                new StartingPartyParticipantSpecification(id, id.equals("human") ? ParticipantType.HUMAN : ParticipantType.BOT,
                        STARTING_GOLD, List.of("base"))).toList(), catalog);
        var session = new GameSession(new GameSessionDefinition(party, roundLimit, new DepositRules(3, 5)));
        var requirements = new ProjectStageRequirements(Map.of(ResourceType.GOLD, STARTING_GOLD,
                ResourceType.PROVISIONS, 0, ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var project = new ProjectDefinition("project", "Project", List.of(new ProjectStageDefinition("stage", "Stage", requirements, "None")));
        return new SoloGameDriver(new SoloGameController(session, project), TURN_SECONDS, TURN_SECONDS);
    }

    @Test void everyFirstSeatFinishesAllTurnsBeforeFinalDepositAndEveryBotGetsItsBonus() {
        for (int roundLimit = 1; roundLimit <= IDS.size(); roundLimit++) {
            var driver = driver(roundLimit);
            for (int round = 1; round <= roundLimit; round++) {
                driver.update(0); // Income -> market.
                String first = IDS.get((round - 1) % IDS.size());
                assertEquals(first, driver.snapshot().currentParticipantId());
                for (var phase : List.of(RoundPhase.MARKET, RoundPhase.PRODUCTION)) {
                    var turns = new ArrayList<String>();
                    for (int i = 0; i < IDS.size(); i++) {
                        assertEquals(phase, driver.snapshot().currentPhase());
                        assertEquals(MatchOutcome.IN_PROGRESS, driver.snapshot().outcome());
                        turns.add(driver.snapshot().currentParticipantId());
                        driver.update(TURN_SECONDS);
                    }
                    assertEquals(IDS.size(), new HashSet<>(turns).size());
                    assertEquals(first, turns.get(0));
                    driver.update(0); // Queue exhausted -> next phase, not another participant action.
                }
                assertEquals(RoundPhase.DEPOSIT, driver.snapshot().currentPhase());
                var before = driver.snapshot().goldBalances();
                driver.update(0);
                before.forEach((id, balance) -> assertEquals(balance + Math.min(5, balance / 3), driver.snapshot().goldBalances().get(id)));
            }
            assertEquals(MatchOutcome.DEFEAT, driver.snapshot().outcome());
        }
    }

    private static final int RESOURCE_TARGET = 1000;
    private static final int BASE_PAYOUT = 1;
    private static final int MAX_UPDATES = 200;
    private static final float BOT_UPDATE_SECONDS = 0.5f;

    @Test void automaticBotActionsFinishEverySeatBeforeTheLastRoundEnds() {
        for (int roundLimit = 1; roundLimit <= IDS.size(); roundLimit++) {
            var driver = productionDriver(roundLimit, testProject(), false, bound -> 0);
            for (int round = 1; round <= roundLimit; round++) {
                driver.update(0);
                String first = IDS.get((round - 1) % IDS.size());
                for (var phase : List.of(RoundPhase.MARKET, RoundPhase.PRODUCTION)) {
                    var completed = new ArrayList<String>();
                    for (int update = 0; driver.snapshot().currentPhase() == phase && update < MAX_UPDATES; update++) {
                        var before = driver.snapshot();
                        assertEquals(MatchOutcome.IN_PROGRESS, before.outcome());
                        if (completed.size() < IDS.size() && before.currentParticipantId().equals(driver.humanId())) {
                            assertTrue(driver.execute(new EndTurnCommand(driver.humanId())).accepted());
                        } else driver.update(BOT_UPDATE_SECONDS);
                        var after = driver.snapshot();
                        if (after.currentPhase() == phase && !before.currentParticipantId().equals(after.currentParticipantId())) {
                            completed.add(before.currentParticipantId());
                        }
                        driver.drainResults().forEach(result -> assertTrue(result.accepted(), result.rejectionReason().orElse("")));
                    }
                    assertEquals(IDS.size(), completed.size());
                    assertEquals(IDS.size(), new HashSet<>(completed).size());
                    assertEquals(first, completed.get(0));
                }
                assertEquals(RoundPhase.DEPOSIT, driver.snapshot().currentPhase());
                driver.snapshot().boards().forEach((id, board) -> {
                    if (!id.equals(driver.humanId())) assertEquals(OptionalInt.of(driver.snapshot().roundNumber()), board.slots().get(0).lastActivatedRound());
                });
                driver.update(0);
            }
            assertEquals(MatchOutcome.DEFEAT, driver.snapshot().outcome());
        }
    }

    @Test void driverPreservesBothCitadelDrawsAndDoesNotPublishUnansweredGold() {
        for (var draws : List.of(List.of(0, 0), List.of(0, 1), List.of(1, 0), List.of(1, 1))) {
            var index = new int[1];
            var driver = productionDriver(1, testProject(), true, bound -> {
                int chunk = index[0]++;
                return chunk % 2 == 0 ? 0 : draws.get(chunk / 2);
            });
            enterProduction(driver);
            var before = driver.snapshot();
            var interaction = driver.beginActivation(0);
            int goldQuestions = 0;
            while (interaction instanceof ActivationInteraction.Prompt question) {
                var prompt = assertInstanceOf(ActivationPrompt.RandomGoldAllocation.class, question.prompt());
                assertEquals(before, driver.snapshot());
                assertEquals(Optional.of(prompt), driver.activationPrompt());
                assertEquals(BASE_PAYOUT, prompt.payout());
                interaction = driver.answerActivation(new ActivationAnswer.Gold(prompt.activationId(), 0));
                goldQuestions++;
            }
            assertInstanceOf(ActivationInteraction.Completed.class, interaction, interaction.toString());
            assertEquals(2 * draws.size(), index[0]);
            assertEquals(Collections.frequency(draws, 0), goldQuestions);
            assertEquals(before.goldBalances().get(driver.humanId()) + goldQuestions,
                    driver.snapshot().goldBalances().get(driver.humanId()));
            assertEquals(Collections.frequency(draws, 1), driver.snapshot().projectProgress().orElseThrow()
                    .contributedResources().get(ResourceType.PROVISIONS));
            assertTrue(driver.activationPrompt().isEmpty());
        }
    }

    @Test void citadelContinuesAfterStageRewardAndStopsImmediatelyOnVictory() {
        var requirements = new ProjectStageRequirements(Map.of(ResourceType.GOLD, BASE_PAYOUT,
                ResourceType.PROVISIONS, 0, ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0), 0);
        var first = new ProjectStageDefinition("first", "First", requirements, "Reward", List.of(),
                List.of(new StageEffect(StageEffect.Kind.CREDIT_EACH, ResourceType.GOLD, 2)));
        var project = new ProjectDefinition("project", "Project", List.of(first, testProject().stages().get(0)));
        var driver = productionDriver(1, project, true, bound -> 0);
        enterProduction(driver);
        var before = driver.snapshot();
        var prompt = assertInstanceOf(ActivationInteraction.Prompt.class, driver.beginActivation(0)).prompt();
        var second = assertInstanceOf(ActivationInteraction.Prompt.class,
                driver.answerActivation(new ActivationAnswer.Gold(prompt.activationId(), BASE_PAYOUT))).prompt();
        assertEquals(before, driver.snapshot());
        var preview = driver.activationProjectProgress().orElseThrow();
        assertEquals(1, preview.completedStageCount());
        assertEquals(Optional.of(testProject().stages().get(0).id()), preview.currentStageId());
        assertInstanceOf(ActivationInteraction.Completed.class,
                driver.answerActivation(new ActivationAnswer.Gold(second.activationId(), 0)));
        assertEquals(1, driver.snapshot().projectProgress().orElseThrow().completedStageCount());
        IDS.forEach(id -> assertEquals(before.goldBalances().get(id) + 2 + (id.equals(driver.humanId()) ? BASE_PAYOUT : 0),
                driver.snapshot().goldBalances().get(id)));
        var committed = driver.snapshot();
        assertInstanceOf(ActivationInteraction.Rejected.class,
                driver.answerActivation(new ActivationAnswer.Gold(second.activationId(), 0)));
        assertEquals(committed, driver.snapshot());

        var draws = new int[1];
        var winning = productionDriver(1, new ProjectDefinition("victory", "Victory", List.of(first)), true,
                bound -> { draws[0]++; return 0; });
        enterProduction(winning);
        var last = assertInstanceOf(ActivationInteraction.Prompt.class, winning.beginActivation(0)).prompt();
        assertInstanceOf(ActivationInteraction.Completed.class,
                winning.answerActivation(new ActivationAnswer.Gold(last.activationId(), BASE_PAYOUT)));
        assertEquals(MatchOutcome.VICTORY, winning.snapshot().outcome());
        assertEquals(2, draws[0]); // One draw consumes two replayable random chunks.
        assertTrue(winning.activationPrompt().isEmpty());
        var victory = winning.snapshot();
        winning.update(TURN_SECONDS);
        assertEquals(victory, winning.snapshot());
    }

    @Test void driverKeepsBothCitadelGoldPayoutsWhenStageGoldIsAlreadyFull() {
        var driver = productionDriver(1, testProject(), true, bound -> 0);
        enterProduction(driver);
        assertTrue(driver.execute(new ContributeGoldCommand(driver.humanId(), RESOURCE_TARGET)).accepted());
        var prompt = assertInstanceOf(ActivationInteraction.Prompt.class, driver.beginActivation(0)).prompt();
        for (int draw = 0; draw < 2; draw++) {
            assertEquals(0, assertInstanceOf(ActivationPrompt.RandomGoldAllocation.class, prompt).maximumProjectGold());
            var interaction = driver.answerActivation(new ActivationAnswer.Gold(prompt.activationId(), 0));
            if (draw == 0) prompt = assertInstanceOf(ActivationInteraction.Prompt.class, interaction).prompt();
            else assertInstanceOf(ActivationInteraction.Completed.class, interaction);
        }
        assertEquals(2, driver.snapshot().goldBalances().get(driver.humanId()));
        assertEquals(RESOURCE_TARGET, driver.snapshot().projectProgress().orElseThrow().contributedResources().get(ResourceType.GOLD));
    }

    @Test void cancellingCitadelDiscardsItsCandidateAndReplaysTheSameDraws() {
        var driver = productionDriver(1, testProject(), true, bound -> 0);
        enterProduction(driver);
        var before = driver.snapshot();
        var first = assertInstanceOf(ActivationInteraction.Prompt.class, driver.beginActivation(0)).prompt();
        driver.answerActivation(new ActivationAnswer.Gold(first.activationId(), 0));
        assertTrue(driver.cancelActivation().accepted());
        assertEquals(before, driver.snapshot());
        assertTrue(driver.activationPrompt().isEmpty());
        var retry = assertInstanceOf(ActivationInteraction.Prompt.class, driver.beginActivation(0)).prompt();
        driver.answerActivation(new ActivationAnswer.Gold(retry.activationId(), 0));
        assertInstanceOf(ActivationInteraction.Completed.class,
                driver.answerActivation(new ActivationAnswer.Gold(retry.activationId(), 0)));
        assertEquals(before.goldBalances().get(driver.humanId()) + 2, driver.snapshot().goldBalances().get(driver.humanId()));
    }

    @Test void loadedFirstStageCreditsEveryParticipantThroughHumanGoldHumanActivationAndBotActivation() {
        for (String completion : List.of("gold", "human", "bot")) {
            var project = loadedProject();
            var driver = productionDriver(1, project, false, bound -> 0);
            enterProduction(driver);
            var requirements = project.stages().get(0).requirements().resources();
            var lastResource = completion.equals("gold") ? ResourceType.GOLD : ResourceType.PROVISIONS;
            for (var resource : ResourceType.values()) {
                int amount = requirements.get(resource) - (resource == lastResource ? BASE_PAYOUT : 0);
                var command = resource == ResourceType.GOLD
                        ? new ContributeGoldCommand(driver.humanId(), amount)
                        : new ContributeResourceCommand(driver.humanId(), resource, amount);
                assertTrue(driver.execute(command).accepted());
            }
            if (completion.equals("bot")) assertTrue(driver.execute(new EndTurnCommand(driver.humanId())).accepted());
            var before = driver.snapshot();
            driver.drainResults();
            if (completion.equals("gold")) assertTrue(driver.execute(new ContributeGoldCommand(driver.humanId(), BASE_PAYOUT)).accepted());
            else if (completion.equals("human")) assertInstanceOf(ActivationInteraction.Completed.class, driver.beginActivation(0));
            else driver.update(BOT_UPDATE_SECONDS);
            var after = driver.snapshot();
            int reward = project.stages().get(0).stageEffects().get(0).amount();
            assertEquals(1, after.projectProgress().orElseThrow().completedStageCount());
            IDS.forEach(id -> assertEquals(before.goldBalances().get(id) + reward
                            - (completion.equals("gold") && id.equals(driver.humanId()) ? BASE_PAYOUT : 0),
                    after.goldBalances().get(id), completion + ": " + id));
            var results = driver.drainResults();
            assertEquals(1, results.size());
            assertTrue(results.get(0).accepted());
            assertEquals(1, results.get(0).snapshot().stageCompletedEvents().size());
            assertEquals(IDS.size(), results.get(0).events().stream()
                    .filter(event -> event instanceof com.myjavacardgame.domain.events.GoldCreditedEvent).count());
            if (!completion.equals("bot")) {
                assertTrue(driver.execute(new ContributeGoldCommand(driver.humanId(), 0)).accepted());
                assertEquals(after.goldBalances(), driver.snapshot().goldBalances());
            }
        }
    }

    private ProjectDefinition testProject() {
        return new ProjectDefinition("project", "Project", List.of(new ProjectStageDefinition("stage", "Stage",
                new ProjectStageRequirements(Map.of(ResourceType.GOLD, RESOURCE_TARGET, ResourceType.PROVISIONS, RESOURCE_TARGET,
                        ResourceType.BUILDING_MATERIALS, RESOURCE_TARGET, ResourceType.SCROLLS, RESOURCE_TARGET), 0), "None")));
    }

    private ProjectDefinition loadedProject() {
        return new ProjectCatalogLoader().load(new java.io.InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/data/fantasy-city-project.json")),
                java.nio.charset.StandardCharsets.UTF_8));
    }

    private SoloGameDriver productionDriver(int roundLimit, ProjectDefinition project, boolean citadel,
            com.myjavacardgame.domain.random.RandomSource random) {
        var catalog = new CardCatalogLoader().load(new java.io.InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/data/card-catalog.json")),
                java.nio.charset.StandardCharsets.UTF_8)).catalog();
        var party = StartingPartyDefinition.fromSpecifications(IDS.stream().map(id ->
                new StartingPartyParticipantSpecification(id, id.equals("human") ? ParticipantType.HUMAN : ParticipantType.BOT,
                        RESOURCE_TARGET, List.of("treasurer"))).toList(), catalog);
        var rules = new com.myjavacardgame.domain.board.BoardRules(6, 3);
        var basic = new CardDefinition("base", "Base", new CardType("test"), Map.of(CardRole.BASE,
                new CardRoleParameters(Map.of(), new CardProduction(Map.of(ResourceType.PROVISIONS, BASE_PAYOUT), List.of()))), "");
        var baseCatalog = new CardCatalog(Map.of("base", basic));
        var boards = new LinkedHashMap<String, com.myjavacardgame.domain.board.BoardSnapshot>();
        for (String id : IDS) {
            var card = CardInstance.fromCatalog(id + "-base", citadel && id.equals("human") ? "citadel" : "base",
                    citadel && id.equals("human") ? catalog : baseCatalog);
            boards.put(id, com.myjavacardgame.domain.board.BoardPlacement.placeBase(
                    com.myjavacardgame.domain.board.BoardSnapshot.empty(), card, rules));
        }
        var session = new GameSession(new GameSessionDefinition(party, roundLimit, new DepositRules(3, 5), project, boards),
                new com.myjavacardgame.domain.effects.DefaultEffectResolver(), rules);
        return new SoloGameDriver(new SoloGameController(session, project, random), TURN_SECONDS, TURN_SECONDS);
    }

    private void enterProduction(SoloGameDriver driver) {
        driver.update(0);
        assertTrue(driver.execute(new EndTurnCommand(driver.humanId())).accepted());
        for (int update = 0; driver.snapshot().currentPhase() == RoundPhase.MARKET && update < MAX_UPDATES; update++) {
            driver.update(BOT_UPDATE_SECONDS);
        }
        assertEquals(RoundPhase.PRODUCTION, driver.snapshot().currentPhase());
        assertEquals(driver.humanId(), driver.snapshot().currentParticipantId());
        driver.drainResults();
    }

    @Test void pauseAndRejectedCommandsDoNotAdvanceOrConsumeTurns() {
        var driver = driver(1);
        driver.update(0);
        driver.setPaused(true);
        var before = driver.snapshot();
        driver.update(TURN_SECONDS);
        assertFalse(driver.execute(new EndTurnCommand("human")).accepted());
        assertEquals(before, driver.snapshot());
        driver.setPaused(false);
        assertFalse(driver.execute(new EndTurnCommand("bot-1")).accepted());
        assertEquals(before, driver.snapshot());
        assertTrue(driver.execute(new EndTurnCommand("human")).accepted());
        assertEquals("bot-1", driver.snapshot().currentParticipantId());
    }
}
