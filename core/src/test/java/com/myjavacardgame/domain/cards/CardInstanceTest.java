package com.myjavacardgame.domain.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CardInstanceTest {
    private static final String CARD_ID = "dual-role";
    private static final String FIRST_INSTANCE_ID = "card-one";
    private static final String SECOND_INSTANCE_ID = "card-two";
    private static final CardRoleParameters BASE_PARAMETERS =
            new CardRoleParameters(Map.of(ResourceType.GOLD, 4));
    private static final CardRoleParameters UPGRADE_PARAMETERS =
            new CardRoleParameters(Map.of(ResourceType.GOLD, 2));

    @Test
    void instancesResolveDefinitionsAndHaveDistinctIdentifiers() {
        CardCatalog catalog = catalog();
        CardInstance first = CardInstance.fromCatalog(FIRST_INSTANCE_ID, CARD_ID, catalog);
        CardInstance second = CardInstance.fromCatalog(SECOND_INSTANCE_ID, CARD_ID, catalog);

        assertSame(catalog.requireCard(CARD_ID), first.definition());
        assertEquals(FIRST_INSTANCE_ID, first.instanceId());
        assertNotEquals(first.instanceId(), second.instanceId());
    }

    @Test
    void placementExposesOnlyTheSelectedRoleParameters() {
        CardInstance card = CardInstance.fromCatalog(FIRST_INSTANCE_ID, CARD_ID, catalog());
        PlacedCard base = new PlacedCard(card, CardRole.BASE);
        PlacedCard upgrade = new PlacedCard(card, CardRole.UPGRADE);

        assertSame(card, base.instance());
        assertEquals(CardRole.BASE, base.role());
        assertEquals(BASE_PARAMETERS, base.roleParameters());
        assertEquals(CardRole.UPGRADE, upgrade.role());
        assertEquals(UPGRADE_PARAMETERS, upgrade.roleParameters());
        assertEquals(CardRole.BASE, base.role());
    }

    @Test
    void sameInstanceCanBePlacedAgainWithAnotherAllowedRole() {
        CardInstance card = CardInstance.fromCatalog(FIRST_INSTANCE_ID, CARD_ID, catalog());
        PlacedCard firstPlacement = new PlacedCard(card, CardRole.BASE);
        PlacedCard laterPlacement = new PlacedCard(firstPlacement.instance(), CardRole.UPGRADE);

        assertSame(firstPlacement.instance(), laterPlacement.instance());
        assertEquals(FIRST_INSTANCE_ID, laterPlacement.instance().instanceId());
        assertEquals(CardRole.BASE, firstPlacement.role());
        assertEquals(CardRole.UPGRADE, laterPlacement.role());
    }

    @Test
    void placementRejectsDisallowedOrMissingRoleAndInstance() {
        CardDefinition baseOnly = new CardDefinition(
                "base-only", "Base", new CardType("type"), Map.of(CardRole.BASE, BASE_PARAMETERS), "");
        CardCatalog catalog = new CardCatalog(Map.of(baseOnly.id(), baseOnly));
        CardInstance card = CardInstance.fromCatalog(FIRST_INSTANCE_ID, baseOnly.id(), catalog);

        assertThrows(IllegalArgumentException.class, () -> new PlacedCard(card, CardRole.UPGRADE));
        assertThrows(NullPointerException.class, () -> new PlacedCard(card, null));
        assertThrows(NullPointerException.class, () -> new PlacedCard(null, CardRole.BASE));
    }

    @Test
    void creationRejectsInvalidIdentifiersAndMissingCatalog() {
        CardCatalog catalog = catalog();

        assertThrows(NullPointerException.class, () -> CardInstance.fromCatalog(null, CARD_ID, catalog));
        assertThrows(IllegalArgumentException.class, () -> CardInstance.fromCatalog(" ", CARD_ID, catalog));
        assertThrows(NullPointerException.class, () -> CardInstance.fromCatalog(FIRST_INSTANCE_ID, null, catalog));
        assertThrows(IllegalArgumentException.class, () -> CardInstance.fromCatalog(FIRST_INSTANCE_ID, "missing", catalog));
        assertThrows(NullPointerException.class, () -> CardInstance.fromCatalog(FIRST_INSTANCE_ID, CARD_ID, null));
    }

    private static CardCatalog catalog() {
        CardDefinition definition = new CardDefinition(
                CARD_ID,
                "Dual role",
                new CardType("type"),
                Map.of(CardRole.BASE, BASE_PARAMETERS, CardRole.UPGRADE, UPGRADE_PARAMETERS),
                "");
        return new CardCatalog(Map.of(definition.id(), definition));
    }
}
