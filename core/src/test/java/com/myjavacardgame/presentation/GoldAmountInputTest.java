package com.myjavacardgame.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GoldAmountInputTest {
    @Test void opensAtMaximumButDoesNotOverwriteAnEditedValue() {
        var input = new GoldAmountInput();
        input.reset(12);
        input.focus();
        input.digit(4);
        input.limit(12);
        assertEquals(4, input.value());
        input.limit(2);
        assertEquals(2, input.value());
    }
    @Test void adjustmentsClampAndCannotOverflow() {
        var input = new GoldAmountInput();
        input.reset(Integer.MAX_VALUE);
        input.adjust(10);
        assertEquals(Integer.MAX_VALUE, input.value());
        input.reset(3);
        input.adjust(-10);
        assertEquals(0, input.value());
    }
    @Test void frameLimitsPreserveFocusAndAllowFurtherTypingAndAdjustments() {
        var input = new GoldAmountInput();
        input.reset(100);
        input.focus();
        input.digit(2);
        input.limit(100);
        assertTrue(input.focused());
        input.digit(5);
        input.limit(100);
        assertEquals(25, input.value());
        input.adjust(-10);
        input.limit(100);
        input.adjust(1);
        assertEquals(16, input.value());
        input.focus();
        input.digit(3);
        assertEquals(3, input.value());
    }

    @Test void zeroCapacityStaysEditableWithoutAllowingAnIllegalAmount() {
        var input = new GoldAmountInput();
        input.reset(0);
        input.focus();
        input.digit(9);
        input.adjust(10);
        assertEquals(0, input.value());
        assertTrue(input.focused());
        input.limit(1);
        input.adjust(1);
        assertEquals(1, input.value());
        input.reset(0);
        assertFalse(input.focused());
        assertEquals(0, input.value());
    }

    @Test void typingReplacesSelectionAndSupportsDeletion() {
        var input = new GoldAmountInput();
        input.reset(100);
        input.focus();
        input.digit(2); input.digit(5);
        assertEquals(25, input.value());
        input.backspace();
        assertEquals(2, input.value());
        input.clear();
        assertEquals(0, input.value());
        input.blur();
        input.digit(9);
        assertEquals(0, input.value());
    }
}
