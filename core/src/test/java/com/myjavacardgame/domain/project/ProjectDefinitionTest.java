package com.myjavacardgame.domain.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectDefinitionTest {
    @Test
    void constructorPreservesStagesInTheSuppliedCompletionOrder() {
        // Arrange
        ProjectStageDefinition secondStage = stage("stage-two");
        ProjectStageDefinition firstStage = stage("stage-one");

        // Act
        ProjectDefinition project = new ProjectDefinition(
                "city-project", "City Project", List.of(secondStage, firstStage));

        // Assert
        assertEquals(
                List.of("stage-two", "stage-one"),
                project.stages().stream().map(ProjectStageDefinition::id).toList());
    }

    @Test
    void constructorCopiesStageListAndExposesAnUnmodifiableSequence() {
        // Arrange
        List<ProjectStageDefinition> sourceStages = new ArrayList<>(List.of(stage("stage-one")));
        ProjectDefinition project = new ProjectDefinition("city-project", "City Project", sourceStages);

        // Act
        sourceStages.add(stage("stage-two"));

        // Assert
        assertEquals(List.of("stage-one"), project.stages().stream().map(ProjectStageDefinition::id).toList());
        assertThrows(UnsupportedOperationException.class, () -> project.stages().add(stage("stage-two")));
    }

    @Test
    void constructorRejectsAnEmptyStageSequence() {
        // Arrange, Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectDefinition("city-project", "City Project", List.of()));
    }

    @Test
    void constructorRejectsDuplicateStageIdentifiers() {
        // Arrange
        List<ProjectStageDefinition> duplicateStages = List.of(stage("stage"), stage("stage"));

        // Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectDefinition("city-project", "City Project", duplicateStages));
    }

    @Test
    void constructorRejectsBlankProjectIdentityFields() {
        // Arrange
        List<ProjectStageDefinition> stages = List.of(stage("stage-one"));

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> new ProjectDefinition(" ", "City Project", stages));
        assertThrows(IllegalArgumentException.class, () -> new ProjectDefinition("city-project", " ", stages));
    }

    private static ProjectStageDefinition stage(String id) {
        return new ProjectStageDefinition(id, "Stage " + id, requirements(), "Complete " + id);
    }

    private static ProjectStageRequirements requirements() {
        Map<ResourceType, Integer> resources = new EnumMap<>(ResourceType.class);
        for (ResourceType resourceType : ResourceType.values()) {
            resources.put(resourceType, 0);
        }
        return new ProjectStageRequirements(resources, 0);
    }
}
