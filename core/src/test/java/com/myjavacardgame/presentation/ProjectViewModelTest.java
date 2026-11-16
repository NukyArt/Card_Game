package com.myjavacardgame.presentation;

import com.myjavacardgame.domain.project.*;
import com.myjavacardgame.domain.resources.ResourceType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectViewModelTest {
    private static final int REQUIRED_GOLD = 10;
    private static final int CONTRIBUTED_GOLD = 3;

    private Map<ResourceType, Integer> amounts(int gold) {
        return Map.of(ResourceType.GOLD, gold, ResourceType.PROVISIONS, 0,
                ResourceType.BUILDING_MATERIALS, 0, ResourceType.SCROLLS, 0);
    }

    private ProjectDefinition project() {
        var requirements = new ProjectStageRequirements(amounts(REQUIRED_GOLD), 0);
        return new ProjectDefinition("project", "Project", List.of(
                new ProjectStageDefinition("first", "First", requirements, "First reward"),
                new ProjectStageDefinition("second", "Second", requirements, "Second reward"),
                new ProjectStageDefinition("third", "Third", requirements, "Victory")));
    }

    @Test void separatesCompletedCurrentAndFutureContributions() {
        var progress = new ProjectProgressSnapshot("project", Optional.of("second"), 1,
                amounts(CONTRIBUTED_GOLD), 0, false);
        var rows = ProjectViewModel.stages(project(), progress);
        assertEquals(3, rows.size());
        assertEquals(ProjectViewModel.Status.COMPLETED, rows.get(0).status());
        assertEquals(REQUIRED_GOLD, rows.get(0).contributedResources().get(ResourceType.GOLD));
        assertEquals(ProjectViewModel.Status.CURRENT, rows.get(1).status());
        assertEquals(CONTRIBUTED_GOLD, rows.get(1).contributedResources().get(ResourceType.GOLD));
        assertEquals(ProjectViewModel.Status.UPCOMING, rows.get(2).status());
        assertEquals(0, rows.get(2).contributedResources().get(ResourceType.GOLD));
        assertEquals("Second reward", rows.get(1).definition().completionEffectDescription());
        assertThrows(UnsupportedOperationException.class, () -> rows.clear());
        assertThrows(UnsupportedOperationException.class,
                () -> rows.get(1).contributedResources().put(ResourceType.GOLD, 0));
    }

    @Test void completedProjectHasNoCurrentOrFutureStage() {
        var progress = new ProjectProgressSnapshot("project", Optional.empty(), 3, amounts(0), 0, true);
        assertTrue(ProjectViewModel.stages(project(), progress).stream()
                .allMatch(stage -> stage.status() == ProjectViewModel.Status.COMPLETED));
    }
}
