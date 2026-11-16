package com.myjavacardgame.presentation;

import com.myjavacardgame.domain.project.ProjectStageCompletedEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectOverlayStateTest {
    private ProjectStageCompletedEvent event(String id, String next) {
        return new ProjectStageCompletedEvent("project", id, Optional.ofNullable(next), next == null);
    }

    @Test void queuesUniqueEventsInOrderAndRestoresRunningStateAfterLastNotice() {
        var state = new ProjectOverlayState();
        var first = event("first", "second");
        var last = event("second", null);
        state.enqueue(List.of(first, last, first), false);
        assertTrue(state.blocking());
        assertEquals(Optional.of(first), state.notice());
        assertTrue(state.closeTop());
        assertEquals(Optional.of(last), state.notice());
        assertFalse(state.closeTop());
        assertFalse(state.blocking());
        state.enqueue(List.of(first, last), false);
        assertFalse(state.blocking());
    }

    @Test void closingOverviewReturnsToNoticeWithoutLosingManualPause() {
        var state = new ProjectOverlayState();
        var first = event("first", "second");
        state.enqueue(List.of(first), true);
        state.openOverview(1, true);
        assertTrue(state.overview());
        assertEquals(1, state.selectedStage());
        assertTrue(state.closeTop());
        assertFalse(state.overview());
        assertEquals(Optional.of(first), state.notice());
        assertTrue(state.closeTop());
        assertFalse(state.blocking());
    }

    @Test void overviewAboveRunningCompletionRestoresRunningOnlyAfterAllNotices() {
        var state = new ProjectOverlayState();
        var first = event("first", "second");
        var last = event("second", null);
        state.enqueue(List.of(first), false);
        state.openOverview(0, true);
        state.enqueue(List.of(first, last), true);
        assertTrue(state.closeTop());
        assertEquals(Optional.of(first), state.notice());
        assertTrue(state.closeTop());
        assertEquals(Optional.of(last), state.notice());
        assertFalse(state.closeTop());
        assertFalse(state.blocking());
    }

    @Test void browsingPreservesOriginalPauseAndDoesNotChangeNotifications() {
        var state = new ProjectOverlayState();
        state.openOverview(0, false);
        state.selectStage(2);
        assertEquals(2, state.selectedStage());
        assertThrows(IllegalArgumentException.class, () -> state.selectStage(-1));
        assertTrue(state.notice().isEmpty());
        assertFalse(state.closeTop());
        state.openOverview(0, true);
        assertTrue(state.closeTop());
    }
}
