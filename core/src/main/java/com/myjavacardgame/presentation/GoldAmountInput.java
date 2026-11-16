package com.myjavacardgame.presentation;

/** Graphics-independent bounded gold editor; focusing selects the existing amount for replacement. */
public final class GoldAmountInput {
    private int value, maximum;
    private boolean focused, replace;

    /** Starts a new allocation at its legal maximum, without retaining a previous prompt's input. */
    public void reset(int maximum) {
        this.maximum = Math.max(0, maximum);
        value = this.maximum;
        focused = false;
        replace = true;
    }
    /** Updates capacity without replacing a deliberate smaller contribution. */
    public void limit(int maximum) {
        this.maximum = Math.max(0, maximum);
        value = Math.min(value, this.maximum);
    }
    /** Selects the current value for replacement by the next typed digit. */
    public void focus() { focused = true; replace = true; }
    /** Releases keyboard ownership. */
    public void blur() { focused = false; }
    /** Returns whether this editor owns numeric keyboard input. */
    public boolean focused() { return focused; }
    /** Returns the legal amount currently displayed. */
    public int value() { return value; }
    /** Adds a decimal digit, safely clamping to current capacity. */
    public void digit(int digit) {
        if (!focused || digit < 0 || digit > 9) return;
        value = (int) Math.min(maximum, (replace ? 0L : (long) value * 10) + digit);
        replace = false;
    }
    /** Deletes one digit or the selected value. */
    public void backspace() { if (focused) { value = replace ? 0 : value / 10; replace = false; } }
    /** Clears the value without releasing keyboard focus. */
    public void clear() { if (focused) { value = 0; replace = false; } }
    /** Adjusts using saturating arithmetic, including -10 and +10. */
    public void adjust(int delta) { value = (int) Math.max(0L, Math.min(maximum, (long) value + delta)); }
}
