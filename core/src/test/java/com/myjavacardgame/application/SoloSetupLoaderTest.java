package com.myjavacardgame.application;

import static org.junit.jupiter.api.Assertions.*;

import com.myjavacardgame.domain.party.ParticipantType;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SoloSetupLoaderTest {
    private String resource(String name) throws IOException {
        try (var input = getClass().getResourceAsStream("/data/" + name)) {
            assertNotNull(input);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private SoloSetupLoader.Data load(String setup, String cards, String project) {
        return new SoloSetupLoader().load(new StringReader(setup), new StringReader(cards), new StringReader(project));
    }

    @Test
    void loadsSoloConfiguration() throws IOException {
        var data = load(resource("solo-setup.json"), resource("card-catalog.json"), resource("fantasy-city-project.json"));
        assertEquals("NORMAL", data.difficulty());
        assertEquals(30, data.roundLimit());
        assertEquals(5, data.marketVisibleSlotCount());
        assertEquals(2, data.marketRefreshGoldCost());
        assertEquals(6, data.boardMaxBases());
        assertEquals(3, data.boardMaxUpgradesPerBase());
        assertEquals(2, data.marketSalePriceDivisor());
        assertEquals(3, data.depositGoldPerBonus());
        assertEquals(5, data.depositMaximumBonus());
        assertEquals(2, data.roundIncomeGold());
        assertEquals(120, data.marketTurnSeconds());
        assertEquals(120, data.productionTurnSeconds());
        assertEquals(4, data.party().participants().size());
        assertEquals(ParticipantType.HUMAN, data.party().participants().get(0).type());
        assertEquals(3, data.party().participants().stream().filter(p -> p.type() == ParticipantType.BOT).count());
        assertTrue(data.party().participants().stream().allMatch(p -> p.startingGold() == 10 && p.startingBaseCards().size() == 4));
        assertEquals(20, data.cards().catalog().definitionsById().size());
        assertEquals(34, data.cards().marketCounts().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(5, data.project().stages().size());
    }

    @Test
    void catalogMatchesApprovedMarketDistribution() throws IOException {
        var data = load(resource("solo-setup.json"), resource("card-catalog.json"), resource("fantasy-city-project.json"));
        // Expected distribution: design/gdd/card-game-gdd.md, section 11.
        var expected = Map.ofEntries(
                Map.entry("treasurer", 1), Map.entry("moneylender", 2), Map.entry("tax-collector", 2),
                Map.entry("inspector", 2), Map.entry("counterfeiter", 1), Map.entry("farm", 1),
                Map.entry("sheep", 3), Map.entry("winery", 2), Map.entry("stables", 2),
                Map.entry("hunting-lodge", 1), Map.entry("sawmill", 1), Map.entry("quarry", 2),
                Map.entry("coal-mine", 2), Map.entry("timber-storehouses", 2), Map.entry("forge", 1),
                Map.entry("apprentice-tower", 1), Map.entry("observatory", 2), Map.entry("laboratory", 2),
                Map.entry("crystal-ball", 3), Map.entry("citadel", 1));
        assertEquals(expected, data.cards().marketCounts());
    }

    @Test
    void loadsConfiguredRoundIncomeInsteadOfHardcodingIt() throws IOException {
        String setup = resource("solo-setup.json").replace("\"roundIncomeGold\": 2", "\"roundIncomeGold\": 7");
        assertEquals(7, load(setup, resource("card-catalog.json"), resource("fantasy-city-project.json")).roundIncomeGold());
    }

    @Test
    void rejectsNegativeRoundIncome() throws IOException {
        String setup = resource("solo-setup.json").replace("\"roundIncomeGold\": 2", "\"roundIncomeGold\": -1");
        assertThrows(IllegalArgumentException.class, () -> load(setup, resource("card-catalog.json"), resource("fantasy-city-project.json")));
    }

    @Test
    void rejectsMissingRoundIncome() throws IOException {
        String setup = resource("solo-setup.json").replace("\"roundIncomeGold\": 2,", "");
        assertThrows(IllegalArgumentException.class, () -> load(setup, resource("card-catalog.json"), resource("fantasy-city-project.json")));
    }

    @Test
    void rejectsInvalidParticipantComposition() throws IOException {
        String setup = resource("solo-setup.json").replace("\"type\": \"HUMAN\"", "\"type\": \"BOT\"");
        assertThrows(IllegalArgumentException.class, () -> load(setup, resource("card-catalog.json"), resource("fantasy-city-project.json")));
    }

    @Test
    void rejectsUnknownStartingCard() throws IOException {
        String cards = resource("card-catalog.json").replace("\"startingCards\":[\"treasurer\"", "\"startingCards\":[\"unknown\"");
        assertThrows(IllegalArgumentException.class, () -> load(resource("solo-setup.json"), cards, resource("fantasy-city-project.json")));
    }

    @Test
    void rejectsIncorrectMarketTotal() throws IOException {
        String cards = resource("card-catalog.json").replaceFirst("\"marketCount\":1", "\"marketCount\":2");
        assertThrows(IllegalArgumentException.class, () -> load(resource("solo-setup.json"), cards, resource("fantasy-city-project.json")));
    }

    @Test
    void rejectsEmptyProject() throws IOException {
        String project = "{\"id\":\"project\",\"name\":\"Project\",\"stages\":[]}";
        assertThrows(IllegalArgumentException.class, () -> load(resource("solo-setup.json"), resource("card-catalog.json"), project));
    }

    @Test
    void rejectsMissingSetupField() throws IOException {
        String setup = resource("solo-setup.json").replace("\"startingGold\": 10,", "");
        assertThrows(IllegalArgumentException.class, () -> load(setup, resource("card-catalog.json"), resource("fantasy-city-project.json")));
    }

    @Test
    void rejectsInvalidConfiguredMarketRule() throws IOException {
        String setup = resource("solo-setup.json").replace("\"marketVisibleSlotCount\": 5", "\"marketVisibleSlotCount\": 0");
        assertThrows(IllegalArgumentException.class, () -> load(setup, resource("card-catalog.json"), resource("fantasy-city-project.json")));
    }
}
