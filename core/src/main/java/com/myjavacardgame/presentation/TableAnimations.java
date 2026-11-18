package com.myjavacardgame.presentation;

import java.util.*;

/** Decorative transitions only: never owns a driver or executes a domain command. */
public final class TableAnimations {
    /** One immutable visual transition, optionally delayed and dealt face-down. */
    public record Motion(String id, TableLayout.Rect from, TableLayout.Rect to, float elapsed, float duration,
            boolean departing, float delay, boolean dealing) {
        /** Returns normalized movement time after the optional deal delay. */
        public float progress() { return duration == 0 ? 1 : Math.max(0, Math.min(1, (elapsed - delay) / duration)); }
        /** Returns smoothstep interpolation at the current visual time. */
        public TableLayout.Rect position() {
            float t = progress();
            t = t * t * (3 - 2 * t);
            return new TableLayout.Rect(from.x() + (to.x() - from.x()) * t, from.y() + (to.y() - from.y()) * t,
                    from.width() + (to.width() - from.width()) * t, from.height() + (to.height() - from.height()) * t);
        }
    }
    private final Map<String, Motion> motions = new LinkedHashMap<>();
    private boolean enabled = true;

    /** Starts or redirects a transition from its current visual position. */
    public void move(String id, TableLayout.Rect from, TableLayout.Rect to, float seconds, boolean departing) {
        start(id, from, to, seconds, departing, 0, false);
    }
    /** Deals a card from the deck with a deterministic stagger and face reveal. */
    public void deal(String id, TableLayout.Rect from, TableLayout.Rect to, float seconds, float delay) {
        start(id, from, to, seconds, false, delay, true);
    }
    private void start(String id, TableLayout.Rect from, TableLayout.Rect to, float seconds, boolean departing, float delay, boolean dealing) {
        if (!Float.isFinite(seconds) || seconds < 0 || !Float.isFinite(delay) || delay < 0) throw new IllegalArgumentException("Invalid animation duration");
        if (!enabled || seconds == 0) { motions.remove(id); return; }
        motions.put(id, new Motion(id, position(id, from), to, 0, seconds, departing, delay, dealing));
    }
    /** Advances by injected frame time, with no wall-clock dependency. */
    public void update(float delta) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid animation delta");
        motions.replaceAll((id, m) -> new Motion(id, m.from(), m.to(), m.elapsed() + delta, m.duration(), m.departing(), m.delay(), m.dealing()));
        motions.values().removeIf(m -> m.elapsed() >= m.delay() + m.duration());
    }
    /** Returns a current position or the authoritative layout when no transition remains. */
    public TableLayout.Rect position(String id, TableLayout.Rect fallback) {
        var motion = motions.get(id);
        return motion == null ? fallback : motion.position();
    }
    /** Returns reveal progress, or one for ordinary face-up cards. */
    public float reveal(String id) {
        var motion = motions.get(id);
        return motion == null || !motion.dealing() ? 1 : motion.progress();
    }
    /** Returns current transitions for noninteractive departing-card rendering. */
    public List<Motion> motions() { return List.copyOf(motions.values()); }
    /** Returns whether an instance is moving. */
    public boolean moving(String id) { return motions.containsKey(id); }
    /** Returns the active count for diagnostics and tests. */
    public int size() { return motions.size(); }
    /** Disables motion immediately, without delaying logical state. */
    public void setEnabled(boolean enabled) { this.enabled = enabled; if (!enabled) clear(); }
    /** Drops transitions when a match or screen is replaced. */
    public void clear() { motions.clear(); }
}
