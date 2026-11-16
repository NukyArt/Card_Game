package com.myjavacardgame.presentation;

import com.myjavacardgame.domain.cards.*;
import com.myjavacardgame.domain.resources.ResourceType;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CardViewModelTest {
    private CardDefinition card(boolean upgrade) {
        var base = new CardRoleParameters(Map.of(ResourceType.GOLD, 4), new CardProduction(Map.of(ResourceType.SCROLLS, 2), List.of()));
        var improvement = new CardRoleParameters(Map.of(ResourceType.GOLD, 1), new CardProduction(Map.of(ResourceType.GOLD, 3), List.of()));
        return new CardDefinition("fixture", "Башня", new CardType("arcana-mysteries"), upgrade ? Map.of(CardRole.BASE, base, CardRole.UPGRADE, improvement) : Map.of(CardRole.BASE, base), "Описание");
    }
    @Test void upgradeStripDependsOnAllowedRoleNotAffordability() {
        var model = CardViewModel.from(card(true));
        assertTrue(model.upgrade().isPresent());
        assertEquals(4, model.base().orElseThrow().prices().get(ResourceType.GOLD));
        assertEquals(1, model.upgrade().orElseThrow().prices().get(ResourceType.GOLD));
        assertEquals(3, model.upgrade().orElseThrow().production().amounts().get(ResourceType.GOLD));
        assertFalse(CardViewModel.from(card(false)).upgrade().isPresent());
    }
    @Test void upgradeOnlyDoesNotInventBaseRole() {
        var definition = card(true);
        var only = new CardDefinition(definition.id(), definition.name(), definition.type(), Map.of(CardRole.UPGRADE, definition.roleParameters().get(CardRole.UPGRADE)), "");
        assertTrue(CardViewModel.from(only).base().isEmpty());
        assertTrue(CardViewModel.from(only).upgrade().isPresent());
    }
}
