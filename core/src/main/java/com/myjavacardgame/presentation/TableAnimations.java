package com.myjavacardgame.presentation;

import java.util.*;

/** Decorative transitions only: never owns a driver or executes a domain command. */
public final class TableAnimations {
    /** One immutable visual transition. */
    public record Motion(String id, TableLayout.Rect from, TableLayout.Rect to, float elapsed, float duration, boolean departing) {
        /** Returns smoothstep interpolation at the current visual time. */
        public TableLayout.Rect position() {
            float t = duration == 0 ? 1 : Math.min(1, elapsed / duration);
            t = t * t * (3 - 2 * t);
            return new TableLayout.Rect(from.x() + (to.x() - from.x()) * t, from.y() + (to.y() - from.y()) * t,
                    from.width() + (to.width() - from.width()) * t, from.height() + (to.height() - from.height()) * t);
        }
    }
    private final Map<String, Motion> motions = new LinkedHashMap<>();
    private boolean enabled = true;

    /** Starts or redirects a transition from its current visual position. */
    public void move(String id, TableLayout.Rect from, TableLayout.Rect to, float seconds, boolean departing) {
        if (!Float.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid animation duration");
        if (!enabled || seconds == 0) { motions.remove(id); return; }
        motions.put(id, new Motion(id, position(id, from), to, 0, seconds, departing));
    }
    /** Advances by injected frame time, with no wall-clock dependency. */
    public void update(float delta) {
        if (!Float.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid animation delta");
        motions.replaceAll((id, m) -> new Motion(id, m.from(), m.to(), m.elapsed() + delta, m.duration(), m.departing()));
        motions.values().removeIf(m -> m.elapsed() >= m.duration());
    }
    /** Returns a current position or the authoritative layout when no transition remains. */
    public TableLayout.Rect position(String id, TableLayout.Rect fallback) {
        var motion = motions.get(id);
        return motion == null ? fallback : motion.position();
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
