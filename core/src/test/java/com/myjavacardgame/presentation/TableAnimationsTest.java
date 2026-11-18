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
    @Test void dealsWaitThenRevealAndFinishUsingInjectedTime() {
        var animations = new TableAnimations();
        animations.deal("first", start, end, 1, 0);
        animations.deal("second", start, end, 1, .25f);
        assertEquals(0, animations.reveal("first"));
        animations.update(.25f);
        assertEquals(.25f, animations.reveal("first"));
        assertEquals(0, animations.reveal("second"));
        assertEquals(start, animations.position("second", end));
        animations.update(.5f);
        assertEquals(.5f, animations.reveal("second"));
        animations.update(.5f);
        assertEquals(0, animations.size());
        assertEquals(1, animations.reveal("second"));
        assertEquals(end, animations.position("second", end));
    }
    @Test void interruptedDealsClearRevealAndDisabledDealsNeverWait() {
        var animations = new TableAnimations();
        animations.deal("card", start, end, 1, 0);
        animations.update(.25f);
        var current = animations.position("card", end);
        animations.move("card", start, end, 1, true);
        assertEquals(current, animations.position("card", end));
        assertEquals(1, animations.reveal("card"));
        animations.setEnabled(false);
        animations.deal("card", start, end, 1, .5f);
        assertEquals(0, animations.size());
        animations.setEnabled(true);
        animations.deal("card", start, end, 0, .5f);
        assertEquals(0, animations.size());
        animations.deal("card", start, end, 1, .5f);
        animations.clear();
        assertEquals(1, animations.reveal("card"));
        assertThrows(IllegalArgumentException.class, () -> animations.deal("bad", start, end, 1, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> animations.deal("bad", start, end, 1, -1));
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
