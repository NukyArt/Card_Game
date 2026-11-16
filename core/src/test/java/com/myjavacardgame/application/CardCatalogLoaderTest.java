package com.myjavacardgame.application;

import static org.junit.jupiter.api.Assertions.*;

import com.myjavacardgame.domain.cards.CardRole;
import com.myjavacardgame.domain.cards.ProductionEffect;
import com.myjavacardgame.domain.resources.ResourceType;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CardCatalogLoaderTest {
    @Test
    void loadsCatalogAndExplicitPendingEffects() {
        var input = getClass().getResourceAsStream("/data/card-catalog.json");
        assertNotNull(input);
        var data = new CardCatalogLoader().load(new InputStreamReader(input, StandardCharsets.UTF_8));
        assertEquals(20, data.catalog().definitionsById().size());
        assertEquals(34, data.marketCounts().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(4, data.startingCards().size());
        assertTrue(data.pendingEffects().isEmpty());
        var ball = data.catalog().requireCard("crystal-ball");
        assertEquals(3, ball.roleParameters().get(CardRole.UPGRADE).production().amounts().get(ResourceType.SCROLLS));
        assertTrue(ball.roleParameters().get(CardRole.UPGRADE).production().effects().isEmpty());
        assertTrue(data.catalog().requireCard("tax-collector").roleParameters().get(CardRole.BASE)
                .production().effects().stream().anyMatch(effect -> effect.kind() == ProductionEffect.Kind.NEXT_SALE_BONUS));
        assertTrue(data.catalog().requireCard("quarry").roleParameters().get(CardRole.BASE)
                .production().effects().stream().noneMatch(effect -> effect.kind() == ProductionEffect.Kind.DEFERRED));
    }

    @Test
    void loadsExpandedUpgradeRolesWithoutChangingMarketCounts() {
        var data = new CardCatalogLoader().load(new InputStreamReader(
                getClass().getResourceAsStream("/data/card-catalog.json"), StandardCharsets.UTF_8));
        assertEquals(9, data.catalog().definitionsById().values().stream()
                .filter(c -> c.roleParameters().containsKey(CardRole.UPGRADE)).count());
        var expectedPrices = java.util.Map.of("moneylender", 3, "winery", 2, "forge", 2, "observatory", 3);
        expectedPrices.forEach((id, price) -> assertEquals(price,
                data.catalog().requireCard(id).roleParameters().get(CardRole.UPGRADE).prices().get(ResourceType.GOLD)));
        var moneylender = data.catalog().requireCard("moneylender").roleParameters().get(CardRole.UPGRADE).production();
        assertEquals(1, moneylender.amounts().get(ResourceType.GOLD));
        assertEquals(ProductionEffect.Kind.CHOICE, moneylender.effects().get(0).kind());
        assertEquals(34, data.marketCounts().values().stream().mapToInt(Integer::intValue).sum());
    }

    @Test
    void rejectsFractionalPrices() {
        String json = "{\"cards\":[{\"id\":\"a\",\"name\":\"A\",\"type\":\"t\",\"marketCount\":1,\"base\":{\"price\":1.5,\"payout\":{\"GOLD\":1}}}],\"startingCards\":[\"a\"]}";
        assertThrows(IllegalArgumentException.class, () -> new CardCatalogLoader().load(new StringReader(json)));
    }

    @Test
    void rejectsMissingCardProperties() {
        assertThrows(IllegalArgumentException.class,
                () -> new CardCatalogLoader().load(new StringReader("{\"cards\":[{}],\"startingCards\":[]}")));
    }
}
