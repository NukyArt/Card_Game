package com.myjavacardgame.application;

import com.myjavacardgame.LaunchOptions;
import com.myjavacardgame.domain.board.BoardRules;
import com.myjavacardgame.domain.market.MarketRules;
import com.myjavacardgame.domain.round.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Integration checks against the same resource fixtures used by the desktop. */
class QaScenarioOutcomeTest {
    private SoloGameDriver driver(String mode) throws Exception {
        var options = LaunchOptions.parse(new String[] {"--qa=" + mode});
        try (var setup = resource(options.setupPath()); var cards = resource("data/card-catalog.json");
                var project = resource(options.projectPath())) {
            var data = new SoloSetupLoader().load(setup, cards, project);
            return new SoloSessionBuilder().buildDriver(data, bound -> 0,
                    new MarketRules(data.marketVisibleSlotCount(), data.marketRefreshGoldCost(),
                            new BoardRules(data.boardMaxBases(), data.boardMaxUpgradesPerBase()), data.marketSalePriceDivisor()),
                    new DepositRules(data.depositGoldPerBonus(), data.depositMaximumBonus(), data.roundIncomeGold()));
        }
    }
    private InputStreamReader resource(String path) {
        return new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path)), StandardCharsets.UTF_8);
    }
    private void reachHumanProduction(SoloGameDriver driver) {
        boolean endedMarket = false;
        for (int step = 0; step < 500; step++) {
            var state = driver.snapshot();
            if (state.currentPhase() == RoundPhase.PRODUCTION && state.currentParticipantId().equals(driver.humanId())) return;
            if (!endedMarket && state.currentPhase() == RoundPhase.MARKET && state.currentParticipantId().equals(driver.humanId())) {
                assertTrue(driver.execute(new EndTurnCommand(driver.humanId())).accepted());
                endedMarket = true;
            }
            driver.update(.5f);
        }
        fail("Human production was not reached");
    }
    @Test void presentationCatalogCoversAllEnumsAndNormalCardTypes() throws Exception {
        try (var reader = resource("i18n/game-text.json"); var setup = resource("data/solo-setup.json");
                var cards = resource("data/card-catalog.json"); var project = resource("data/fantasy-city-project.json")) {
            var words = new com.myjavacardgame.presentation.RussianGameText(reader);
            for (var phase : RoundPhase.values()) assertFalse(words.label("phase", phase).isBlank());
            for (var role : com.myjavacardgame.domain.cards.CardRole.values()) assertFalse(words.label("role", role).isBlank());
            for (var resource : com.myjavacardgame.domain.resources.ResourceType.values()) assertFalse(words.label("resource", resource).isBlank());
            for (var effect : com.myjavacardgame.domain.cards.ProductionEffect.Kind.values()) assertFalse(words.label("effect", effect).isBlank());
            var data = new SoloSetupLoader().load(setup, cards, project);
            for (var card : data.cards().catalog().definitionsById().values()) {
                assertFalse(words.text("type." + card.type().id()).isBlank());
                assertTrue(card.name().matches(".*[А-Яа-яЁё].*"));
            }
            assertEquals("Недостаточно золота для покупки.", words.rejection("The participant does not have enough gold."));
            assertEquals("Игрок", words.participant("player"));
            assertEquals("Бот 3", words.participant("bot-3"));
        }
    }

    @Test void victoryComesFromFiveGenuineStageContributions() throws Exception {
        var driver = driver("victory");
        reachHumanProduction(driver);
        assertEquals(0, driver.snapshot().projectProgress().orElseThrow().completedStageCount());
        driver.beginActivation(0);
        var prompt = driver.activationPrompt().orElseThrow();
        driver.answerActivation(new ActivationAnswer.Gold(prompt.activationId(), 4));
        assertEquals(1, driver.snapshot().projectProgress().orElseThrow().completedStageCount());
        for (int base = 1; base < 4; base++) {
            driver.beginActivation(base);
            assertEquals(base + 1, driver.snapshot().projectProgress().orElseThrow().completedStageCount());
        }
        assertTrue(driver.execute(new ContributeBoardPackageCommand(driver.humanId(), 0)).accepted());
        assertEquals(5, driver.snapshot().projectProgress().orElseThrow().completedStageCount());
        assertEquals(MatchOutcome.VICTORY, driver.snapshot().outcome());
    }
    @Test void defeatComesFromCompletingTheOnlyRound() throws Exception {
        var driver = driver("defeat");
        var endedPhases = java.util.EnumSet.noneOf(RoundPhase.class);
        for (int step = 0; step < 1000 && driver.snapshot().outcome() == MatchOutcome.IN_PROGRESS; step++) {
            var state = driver.snapshot();
            if ((state.currentPhase() == RoundPhase.MARKET || state.currentPhase() == RoundPhase.PRODUCTION)
                    && state.currentParticipantId().equals(driver.humanId()) && endedPhases.add(state.currentPhase())) {
                assertTrue(driver.execute(new EndTurnCommand(driver.humanId())).accepted());
            }
            driver.update(.5f);
        }
        assertEquals(1, driver.snapshot().roundNumber());
        assertEquals(MatchOutcome.DEFEAT, driver.snapshot().outcome());
    }
}
