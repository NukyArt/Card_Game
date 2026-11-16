package com.myjavacardgame.domain.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.myjavacardgame.domain.cards.CardCatalog;
import com.myjavacardgame.domain.cards.CardDefinition;
import com.myjavacardgame.domain.cards.CardInstance;
import com.myjavacardgame.domain.cards.CardRole;
import com.myjavacardgame.domain.cards.CardRoleParameters;
import com.myjavacardgame.domain.cards.CardType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BoardSnapshotTest {
    private static final String CARD_ID = "dual-role";
    private static final String BASE_ID = "base-instance";
    private static final String UPGRADE_ID = "upgrade-instance";
    private static final BoardRules RULES = new BoardRules(6, 3);

    @Test
    void snapshotCollectionsCannotBeModifiedAndEarlierSnapshotsRemainUnchanged() {
        CardRoleParameters parameters = new CardRoleParameters(Map.of());
        CardDefinition definition = new CardDefinition(
                CARD_ID, "Dual role", new CardType("type"),
                Map.of(CardRole.BASE, parameters, CardRole.UPGRADE, parameters), "");
        CardCatalog catalog = new CardCatalog(Map.of(CARD_ID, definition));
        CardInstance base = CardInstance.fromCatalog(BASE_ID, CARD_ID, catalog);
        CardInstance upgrade = CardInstance.fromCatalog(UPGRADE_ID, CARD_ID, catalog);

        BoardSnapshot empty = BoardSnapshot.empty();
        BoardSnapshot withBase = BoardPlacement.placeBase(empty, base, RULES);
        BoardSnapshot withUpgrade = BoardPlacement.placeUpgrade(withBase, upgrade, 0, RULES);

        assertThrows(UnsupportedOperationException.class, () -> withUpgrade.slots().clear());
        assertThrows(UnsupportedOperationException.class, () -> withUpgrade.slots().get(0).upgrades().clear());
        assertEquals(0, empty.slots().size());
        assertEquals(1, withBase.slots().size());
        assertEquals(0, withBase.slots().get(0).upgrades().size());
        assertEquals(1, withUpgrade.slots().size());
        assertEquals(1, withUpgrade.slots().get(0).upgrades().size());
        assertSame(base, withUpgrade.slots().get(0).base().instance());
        assertSame(upgrade, withUpgrade.slots().get(0).upgrades().get(0).instance());
    }
}
