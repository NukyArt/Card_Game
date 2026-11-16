package com.myjavacardgame.domain.project;

import com.myjavacardgame.domain.effects.EffectDefinition;
import java.util.List;
import java.util.Objects;

/**
 * Immutable definition of one ordered stage in a project.
 *
 * @param id stable, nonblank stage identifier
 * @param displayName nonblank name displayed for the stage
 * @param requirements immutable requirements needed to complete the stage
 * @param completionEffectDescription nonblank description of the effect applied at completion
 */
public record ProjectStageDefinition(
        String id,
        String displayName,
        ProjectStageRequirements requirements,
        String completionEffectDescription,
        List<EffectDefinition> completionEffects,
        List<StageEffect> stageEffects) {
    /** Retains the existing structured-effect constructor. */
    public ProjectStageDefinition(String id, String displayName, ProjectStageRequirements requirements,
            String completionEffectDescription, List<EffectDefinition> completionEffects) {
        this(id, displayName, requirements, completionEffectDescription, completionEffects, List.of());
    }
    /** Creates a stage without structured effects for existing callers. */
    public ProjectStageDefinition(String id, String displayName, ProjectStageRequirements requirements,
            String completionEffectDescription) {
        this(id, displayName, requirements, completionEffectDescription, List.of());
    }
    /**
     * Creates a project-stage definition.
     *
     * @param id stable, nonblank stage identifier
     * @param displayName nonblank name displayed for the stage
     * @param requirements non-null completion requirements
     * @param completionEffectDescription nonblank completion-effect description
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if an identifier, name, or effect description is blank
     */
    public ProjectStageDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(requirements, "requirements");
        Objects.requireNonNull(completionEffectDescription, "completionEffectDescription");
        completionEffects = List.copyOf(Objects.requireNonNull(completionEffects, "completionEffects"));
        stageEffects = List.copyOf(Objects.requireNonNull(stageEffects, "stageEffects"));
        if (id.isBlank() || displayName.isBlank() || completionEffectDescription.isBlank()) {
            throw new IllegalArgumentException("Stage id, display name, and effect description must not be blank");
        }
    }
}
