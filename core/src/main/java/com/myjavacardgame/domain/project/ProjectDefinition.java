package com.myjavacardgame.domain.project;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable project definition containing its stages in completion order.
 *
 * @param id stable, nonblank project identifier
 * @param displayName nonblank name displayed for the project
 * @param stages nonempty, immutable sequence of uniquely identified stages
 */
public record ProjectDefinition(String id, String displayName, List<ProjectStageDefinition> stages) {
    /**
     * Creates a project definition with an ordered immutable stage sequence.
     *
     * @param id stable, nonblank project identifier
     * @param displayName nonblank name displayed for the project
     * @param stages nonempty sequence of non-null stages with unique identifiers
     * @throws NullPointerException if an argument or a stage is null
     * @throws IllegalArgumentException if an identifier or name is blank, the sequence is empty,
     *                                  or stage identifiers are duplicated
     */
    public ProjectDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(stages, "stages");
        if (id.isBlank() || displayName.isBlank()) {
            throw new IllegalArgumentException("Project id and display name must not be blank");
        }
        stages = List.copyOf(stages);
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("A project must contain at least one stage");
        }
        Set<String> stageIds = new HashSet<>();
        for (ProjectStageDefinition stage : stages) {
            if (!stageIds.add(stage.id())) {
                throw new IllegalArgumentException("Project stage identifiers must be unique: " + stage.id());
            }
        }
    }
}
