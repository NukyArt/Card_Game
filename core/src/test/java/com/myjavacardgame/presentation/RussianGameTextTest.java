package com.myjavacardgame.presentation;

import java.io.StringReader;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RussianGameTextTest {
    private RussianGameText fixture() {
        return new RussianGameText(new StringReader("""
                {"greeting":{"en":"Round {round}","ru":"Раунд {round}","context":"Header; 600px"},
                 "resource.GOLD":{"en":"Gold","ru":"Золото","context":"Resource; 300px"},
                 "unavailable":{"en":"Unavailable","ru":"Действие недоступно","context":"Feedback; 1240px"},
                 "reject.Not enough gold":{"en":"Not enough gold","ru":"Недостаточно золота","context":"Feedback; 1240px"}}
                """));
    }
    @Test void formatsNamedPlaceholdersAndResourceAmounts() {
        var text = fixture();
        assertEquals("Раунд 2", text.format("greeting", Map.of("round", 2)));
        assertEquals("Золото: 4", text.amounts(Map.of(com.myjavacardgame.domain.resources.ResourceType.GOLD, 4)));
    }
    @Test void translatesRejectionsWithoutLeakingUnknownEnglish() {
        var text = fixture();
        assertEquals("Недостаточно золота", text.rejection("Not enough gold"));
        assertEquals("Действие недоступно", text.rejection("Future technical message"));
    }
    @Test void rejectsMissingOrMismatchedPlaceholders() {
        assertThrows(IllegalArgumentException.class, () -> fixture().format("greeting", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new RussianGameText(new StringReader("""
                {"bad":{"en":"Value {value}","ru":"Значение {other}","context":"Feedback; 1240px"}}
                """)));
    }
}
