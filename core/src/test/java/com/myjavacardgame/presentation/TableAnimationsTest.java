package com.myjavacardgame.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TableAnimationsTest {
    private final TableLayout.Rect start = new TableLayout.Rect(0, 0, 64, 100);
    private final TableLayout.Rect end = new TableLayout.Rect(200, 100, 64, 100);
    @Test void transitionsFinishAndResetWithoutSideEffects() {
        var animations = new TableAnimations();
        animations.move("card", start, end, 1, false);
        assertEquals(start, animations.position("card", end));
        animations.update(.5f);
        assertEquals(100, animations.position("card", end).x(), .001f);
        animations.update(.5f);
        assertEquals(end, animations.position("card", end));
        assertEquals(0, animations.size());
        animations.move("other", start, end, 1, true);
        animations.clear();
        assertEquals(0, animations.size());
    }
    @Test void disabledMotionAndZeroDurationCompleteImmediately() {
        var animations = new TableAnimations();
        animations.move("card", start, end, 0, false);
        assertEquals(end, animations.position("card", end));
        animations.move("card", start, end, 1, false);
        animations.setEnabled(false);
        assertEquals(end, animations.position("card", end));
        assertEquals(0, animations.size());
        assertThrows(IllegalArgumentException.class, () -> animations.update(Float.NaN));
    }
    @Test void replacingTransitionStartsFromItsCurrentVisualLocation() {
        var animations = new TableAnimations();
        animations.move("card", start, end, 1, false);
        animations.update(.5f);
        var current = animations.position("card", end);
        animations.move("card", start, start, 1, false);
        assertEquals(current, animations.position("card", start));
    }
}
