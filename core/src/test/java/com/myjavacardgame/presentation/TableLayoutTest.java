package com.myjavacardgame.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TableLayoutTest {
    @Test void sixHumanBasesStayInOneUnobstructedRow() {
        var layout = new TableLayout(TableStyle.defaults());
        for (int i = 0; i < 6; i++) {
            var card = layout.board(0, i);
            assertEquals(layout.board(0, 0).y(), card.y());
            assertEquals(.64f, card.width() / card.height(), .001f);
            assertTrue(card.x() >= 0 && card.right() <= TableLayout.WIDTH);
            if (i > 0) assertTrue(layout.board(0, i - 1).right() < card.x());
            assertFalse(card.overlaps(layout.project()));
        }
    }
    @Test void allBotCardsStayOutsideCenterAndInsideTable() {
        var layout = new TableLayout(TableStyle.defaults());
        for (int seat = 1; seat <= 3; seat++) for (int i = 0; i < 6; i++) {
            var card = layout.board(seat, i);
            assertTrue(card.x() >= 0 && card.y() >= 0 && card.right() <= TableLayout.WIDTH && card.top() <= TableLayout.HEIGHT);
            assertFalse(card.overlaps(layout.project()));
            for (int m = 0; m < 5; m++) assertFalse(card.overlaps(layout.market(m, 5)));
        }
    }
    @Test void letterboxUsesSameScaleForBothAxes() {
        var viewport = TableLayout.viewport(1600, 1000);
        assertEquals(0, viewport.x(viewport.offsetX()), .001f);
        assertEquals(720, viewport.y(viewport.offsetY()), .001f);
        assertEquals(640, viewport.x(800), .001f);
        assertEquals(360, viewport.y(500), .001f);
        assertFalse(new TableLayout.Rect(0, 0, 10, 10).contains(10, 5));
    }
    @Test void invalidIndicesAndWindowSizesAreRejected() {
        var layout = new TableLayout(TableStyle.defaults());
        assertThrows(IllegalArgumentException.class, () -> layout.board(0, 6));
        assertThrows(IllegalArgumentException.class, () -> TableLayout.viewport(0, 720));
    }
}
