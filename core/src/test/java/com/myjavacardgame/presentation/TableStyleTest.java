package com.myjavacardgame.presentation;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TableStyleTest {
    @Test void loadsFromInjectedReader() {
        var style = TableStyle.load(new StringReader("""
            {"cardAspect":0.64,"cardHeight":154,"gap":10,"hoverLift":12,"hoverScale":1.05,
             "hoverSeconds":0.15,"moveSeconds":0.45,"effectSeconds":0.65,"animations":true,
             "tableColor":"102C38","panelColor":"172B36","accentColor":"D7B770"}
            """));
        assertEquals(154, style.cardHeight());
        assertTrue(style.animations());
    }
    @Test void defaultsUseTheWarmLightTheme() {
        assertEquals("EFC98D", TableStyle.defaults().tableColor());
        assertEquals("FFF0CE", TableStyle.defaults().panelColor());
        assertEquals("F7B253", TableStyle.defaults().accentColor());
        assertThrows(IllegalArgumentException.class, () -> new TableStyle(.64f, 154, 10, 12, 1.05f, .15f, .45f, .65f, true, "not-hex", "FFF0CE", "F7B253"));
    }
    @Test void rejectsImpossibleGeometryAndInvalidTiming() {
        assertThrows(IllegalArgumentException.class, () -> new TableStyle(.64f, -1, 10, 12, 1.05f, .15f, .45f, .65f, true, "102C38", "172B36", "D7B770"));
        assertThrows(IllegalArgumentException.class, () -> new TableStyle(.64f, 154, 10, 12, 1.05f, Float.NaN, .45f, .65f, true, "102C38", "172B36", "D7B770"));
    }
}
