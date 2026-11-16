package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.project.ProjectProgress;
import com.myjavacardgame.domain.project.ProjectStageCompletedEvent;
import com.myjavacardgame.domain.project.StageEffect;
import com.myjavacardgame.domain.resources.ResourceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Private transaction state for project contributions and stage completion effects. */
final class StageResolution {
    final ProjectProgress progress;
    final Map<String, Integer> balances;
    final List<StageEffect> modifiers;
    final List<ProjectStageCompletedEvent> events = new ArrayList<>();
    final List<com.myjavacardgame.domain.events.DomainEvent> domainEvents = new ArrayList<>();
    final List<com.myjavacardgame.domain.cards.ProductionEffect> marketEffects = new ArrayList<>();

    StageResolution(ProjectProgress progress, Map<String, Integer> balances, List<StageEffect> modifiers) {
        this.progress = progress.copy();
        this.balances = new LinkedHashMap<>(balances);
        this.modifiers = new ArrayList<>(modifiers);
    }

    void contribute(ResourceType resource, int amount) {
        if (!progress.isCompleted() && amount > 0) resolve(progress.contribute(resource, amount));
    }

    void contributeCards(int amount) {
        if (!progress.isCompleted() && amount > 0) resolve(progress.contributeCards(amount));
    }

    private void resolve(List<ProjectStageCompletedEvent> completed) {
        for (var event : completed) {
            events.add(event);
            domainEvents.add(event);
            if (event.projectCompleted()) return;
            for (StageEffect effect : progress.stageDefinition(event.completedStageId()).stageEffects()) {
                if (progress.isCompleted()) return;
                switch (effect.kind()) {
                    case CREDIT_EACH -> {
                        for (var entry : balances.entrySet()) {
                            entry.setValue(Math.addExact(entry.getValue(), effect.amount()));
                            if (effect.amount() > 0) {
                                domainEvents.add(new com.myjavacardgame.domain.events.GoldCreditedEvent(entry.getKey(), effect.amount()));
                            }
                        }
                    }
                    case MODIFY_PRODUCTION -> modifiers.add(effect);
                    case CONTRIBUTE_EACH -> {
                        for (int i = 0; i < balances.size() && !progress.isCompleted(); i++) {
                            contribute(effect.resource(), effect.amount());
                        }
                    }
                }
            }
        }
    }
}
