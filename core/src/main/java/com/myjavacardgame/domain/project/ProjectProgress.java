package com.myjavacardgame.domain.project;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable clean-domain progress tracker for one data-defined project.
 * Contributions are capped to the current stage, so excess never reaches a later stage.
 */
public final class ProjectProgress {
    private final ProjectDefinition definition;
    private int stageIndex;
    private EnumMap<ResourceType, Integer> resources;
    private int cards;

    /** Creates progress at the first configured stage. */
    public ProjectProgress(ProjectDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.resources = zeroResources();
    }

    /** Returns an independent copy suitable for preparing an atomic operation. */
    public ProjectProgress copy() {
        ProjectProgress copy = new ProjectProgress(definition);
        copy.stageIndex = stageIndex;
        copy.resources = new EnumMap<>(resources);
        copy.cards = cards;
        return copy;
    }

    /** Returns the configured stage definition for a completion event. */
    public ProjectStageDefinition stageDefinition(String stageId) {
        Objects.requireNonNull(stageId, "stageId");
        return definition.stages().stream().filter(stage -> stage.id().equals(stageId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown stage: " + stageId));
    }

    /** Returns an upper bound on gold credited to each participant by remaining stages. */
    public long maximumRemainingGoldCredit() {
        return definition.stages().subList(stageIndex, definition.stages().size()).stream()
                .flatMap(stage -> stage.stageEffects().stream())
                .filter(effect -> effect.kind() == StageEffect.Kind.CREDIT_EACH)
                .mapToLong(StageEffect::amount).sum();
    }

    /** Returns the remaining demand for a resource in the current stage, or zero after completion. */
    public int remaining(ResourceType resource) {
        Objects.requireNonNull(resource, "resource");
        if (isCompleted()) {
            return 0;
        }
        return definition.stages().get(stageIndex).requirements().resources().get(resource) - resources.get(resource);
    }

    /** Returns the remaining card demand for the current stage, or zero after completion. */
    public int remainingCards() {
        return isCompleted() ? 0 : definition.stages().get(stageIndex).requirements().requiredCardCount() - cards;
    }

    /** Applies a nonnegative resource contribution and returns all stages immediately completed by it. */
    public List<ProjectStageCompletedEvent> contribute(ResourceType resource, int amount) {
        Objects.requireNonNull(resource, "resource");
        if (amount < 0) {
            throw new IllegalArgumentException("Contribution must not be negative");
        }
        if (isCompleted()) {
            throw new IllegalStateException("The project has already completed");
        }
        resources.put(resource, resources.get(resource) + Math.min(amount, remaining(resource)));
        return completeIfReady();
    }

    /** Applies a nonnegative card-package contribution and returns immediately completed stages. */
    public List<ProjectStageCompletedEvent> contributeCards(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Card contribution must not be negative");
        }
        if (isCompleted()) {
            throw new IllegalStateException("The project has already completed");
        }
        cards += Math.min(amount, remainingCards());
        return completeIfReady();
    }

    /** Returns an immutable snapshot. */
    public ProjectProgressSnapshot snapshot() {
        return new ProjectProgressSnapshot(
                definition.id(),
                isCompleted() ? java.util.Optional.empty() : java.util.Optional.of(definition.stages().get(stageIndex).id()),
                stageIndex,
                resources,
                cards,
                isCompleted());
    }

    /** Returns whether every configured stage has completed. */
    public boolean isCompleted() {
        return stageIndex == definition.stages().size();
    }

    private List<ProjectStageCompletedEvent> completeIfReady() {
        if (!isCurrentStageComplete()) {
            return List.of();
        }
        ProjectStageDefinition completedStage = definition.stages().get(stageIndex++);
        resources = zeroResources();
        cards = 0;
        return List.of(new ProjectStageCompletedEvent(
                definition.id(), completedStage.id(),
                isCompleted() ? java.util.Optional.empty() : java.util.Optional.of(definition.stages().get(stageIndex).id()),
                isCompleted()));
    }

    private boolean isCurrentStageComplete() {
        return remainingCards() == 0 && EnumSet.allOf(ResourceType.class).stream().allMatch(resource -> remaining(resource) == 0);
    }

    private static EnumMap<ResourceType, Integer> zeroResources() {
        EnumMap<ResourceType, Integer> values = new EnumMap<>(ResourceType.class);
        for (ResourceType resource : ResourceType.values()) {
            values.put(resource, 0);
        }
        return values;
    }
}
