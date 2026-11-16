package com.myjavacardgame.domain.project;

import com.myjavacardgame.domain.events.DomainEvent;
import java.util.Objects;
import java.util.Optional;

/** Immutable notification emitted after a stage opens its successor and before its effect is resolved. */
public record ProjectStageCompletedEvent(
        String projectId, String completedStageId, Optional<String> openedStageId, boolean projectCompleted) implements DomainEvent {
    /** Validates the event's data-defined stage identifiers. */
    public ProjectStageCompletedEvent {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(completedStageId, "completedStageId");
        Objects.requireNonNull(openedStageId, "openedStageId");
        if (projectId.isBlank() || completedStageId.isBlank() || projectCompleted != openedStageId.isEmpty()) {
            throw new IllegalArgumentException("Stage completion event values are inconsistent");
        }
    }
}
