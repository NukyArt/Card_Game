package com.myjavacardgame.presentation;

import com.myjavacardgame.domain.project.ProjectDefinition;
import com.myjavacardgame.domain.project.ProjectProgressSnapshot;
import com.myjavacardgame.domain.project.ProjectStageDefinition;
import com.myjavacardgame.domain.resources.ResourceType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Read-only, graphics-independent rows for the project browser and activation context. */
public final class ProjectViewModel {
    private ProjectViewModel() { }

    /** Describes a stage relative to the supplied committed or candidate progress. */
    public enum Status { COMPLETED, CURRENT, UPCOMING }

    /** Immutable stage details; completed rows use their own requirements, never a successor's contributions. */
    public record Stage(ProjectStageDefinition definition, Status status,
            Map<ResourceType, Integer> contributedResources, int contributedCards) {
        /** Copies resource values so a view cannot change session state. */
        public Stage { contributedResources = Map.copyOf(contributedResources); }
    }

    /** Builds one ordered row per data-defined stage without changing project progress. */
    public static List<Stage> stages(ProjectDefinition project, ProjectProgressSnapshot progress) {
        if (!project.id().equals(progress.projectId())) throw new IllegalArgumentException("Project does not match progress");
        var stages = new ArrayList<Stage>();
        for (int index = 0; index < project.stages().size(); index++) {
            var definition = project.stages().get(index);
            var status = index < progress.completedStageCount() ? Status.COMPLETED
                    : progress.currentStageId().filter(definition.id()::equals).isPresent() ? Status.CURRENT : Status.UPCOMING;
            var amounts = new EnumMap<ResourceType, Integer>(ResourceType.class);
            for (var resource : ResourceType.values()) amounts.put(resource, switch (status) {
                case COMPLETED -> definition.requirements().resources().get(resource);
                case CURRENT -> progress.contributedResources().get(resource);
                case UPCOMING -> 0;
            });
            int cards = switch (status) {
                case COMPLETED -> definition.requirements().requiredCardCount();
                case CURRENT -> progress.contributedCardCount();
                case UPCOMING -> 0;
            };
            stages.add(new Stage(definition, status, amounts, cards));
        }
        return List.copyOf(stages);
    }
}
