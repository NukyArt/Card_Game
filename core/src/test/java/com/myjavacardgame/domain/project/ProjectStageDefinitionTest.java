package com.myjavacardgame.domain.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectStageDefinitionTest {
    @Test
    void requirementsCopiesResourceMapAndExposesAnUnmodifiableMap() {
        // Arrange
        Map<ResourceType, Integer> sourceResources = completeResources(10);
        ProjectStageRequirements requirements = new ProjectStageRequirements(sourceResources, 2);

        // Act
        sourceResources.put(ResourceType.GOLD, 99);

        // Assert
        assertEquals(10, requirements.resources().get(ResourceType.GOLD));
        assertThrows(
                UnsupportedOperationException.class,
                () -> requirements.resources().put(ResourceType.GOLD, 99));
    }

    @Test
    void requirementsRejectNegativeAmountForEveryResourceType() {
        // Arrange and Act / Assert
        for (ResourceType resourceType : ResourceType.values()) {
            Map<ResourceType, Integer> resources = completeResources(0);
            resources.put(resourceType, -1);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> new ProjectStageRequirements(resources, 0),
                    () -> "Expected a negative " + resourceType + " requirement to be rejected");
        }
    }

    @Test
    void requirementsRejectIncompleteResourceMappingAndNegativeCardCount() {
        // Arrange
        Map<ResourceType, Integer> incompleteResources = new EnumMap<>(ResourceType.class);
        incompleteResources.put(ResourceType.GOLD, 0);

        // Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectStageRequirements(incompleteResources, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectStageRequirements(completeResources(0), -1));
    }

    @Test
    void constructorRejectsBlankIdentityAndCompletionEffectFields() {
        // Arrange
        ProjectStageRequirements requirements = new ProjectStageRequirements(completeResources(0), 0);

        // Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectStageDefinition(" ", "Stage", requirements, "Effect"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectStageDefinition("stage", " ", requirements, "Effect"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectStageDefinition("stage", "Stage", requirements, " "));
    }

    private static Map<ResourceType, Integer> completeResources(int amount) {
        Map<ResourceType, Integer> resources = new EnumMap<>(ResourceType.class);
        for (ResourceType resourceType : ResourceType.values()) {
            resources.put(resourceType, amount);
        }
        return resources;
    }
}
