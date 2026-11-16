package com.myjavacardgame.domain.project;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable externally safe view of the current project stage and its contributions. */
public record ProjectProgressSnapshot(
        String projectId,
        Optional<String> currentStageId,
        int completedStageCount,
        Map<ResourceType, Integer> contributedResources,
        int contributedCardCount,
        boolean completed) {
    /** Defensively copies contribution data. */
    public ProjectProgressSnapshot {
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(currentStageId, "currentStageId");
        Objects.requireNonNull(contributedResources, "contributedResources");
        if (projectId.isBlank() || completedStageCount < 0 || contributedCardCount < 0) {
            throw new IllegalArgumentException("Project progress values are invalid");
        }
        EnumMap<ResourceType, Integer> copy = new EnumMap<>(ResourceType.class);
        copy.putAll(contributedResources);
        if (!copy.keySet().equals(java.util.EnumSet.allOf(ResourceType.class))
                || copy.values().stream().anyMatch(value -> value == null || value < 0)) {
            throw new IllegalArgumentException("Contributions must cover every resource with nonnegative values");
        }
        contributedResources = Map.copyOf(copy);
        if (completed != currentStageId.isEmpty()) {
            throw new IllegalArgumentException("Completed status must agree with the current stage");
        }
    }
}
