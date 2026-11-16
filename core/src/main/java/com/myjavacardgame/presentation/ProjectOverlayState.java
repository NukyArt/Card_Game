package com.myjavacardgame.presentation;

import com.myjavacardgame.domain.project.ProjectStageCompletedEvent;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Ordered project windows and the manual pause state to restore after their last dismissal. */
public final class ProjectOverlayState {
    private final ArrayDeque<ProjectStageCompletedEvent> notices = new ArrayDeque<>();
    private final Set<ProjectStageCompletedEvent> seen = new HashSet<>();
    private boolean overview, previouslyPaused;
    private int selectedStage;

    /** Adds each stage completion once, retaining the original pre-overlay pause. */
    public void enqueue(List<ProjectStageCompletedEvent> events, boolean paused) {
        for (var event : events) {
            if (!seen.add(event)) continue;
            if (!blocking()) previouslyPaused = paused;
            notices.addLast(event);
        }
    }

    /** Opens the browser above any existing notification without replacing its pause context. */
    public void openOverview(int stage, boolean paused) {
        selectStage(stage);
        if (!blocking()) previouslyPaused = paused;
        overview = true;
    }

    /** Selects a nonnegative stage index; the caller bounds it against the project definition. */
    public void selectStage(int stage) {
        if (stage < 0) throw new IllegalArgumentException("Stage index must be nonnegative");
        selectedStage = stage;
    }

    /** Returns the browser's selected stage. */
    public int selectedStage() { return selectedStage; }
    /** Returns whether the browser is the top project window. */
    public boolean overview() { return overview; }
    /** Returns the next completion in chronological order. */
    public Optional<ProjectStageCompletedEvent> notice() { return Optional.ofNullable(notices.peekFirst()); }
    /** Returns whether project windows must block gameplay and timers. */
    public boolean blocking() { return overview || !notices.isEmpty(); }

    /** Closes only the top window and returns the pause value to apply to the driver. */
    public boolean closeTop() {
        if (overview) overview = false;
        else notices.pollFirst();
        return blocking() || previouslyPaused;
    }
}
