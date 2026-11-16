package com.myjavacardgame.domain.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Stateless resolver for the supported structured effects. */
public final class DefaultEffectResolver implements EffectResolver {
    /** Creates a resolver. */
    public DefaultEffectResolver() { }

    @Override
    public EffectResolutionResult resolve(EffectResolutionState state, EffectResolutionRequest request) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(request, "request");
        Map<String, String> supplied = new HashMap<>();
        for (EffectChoice choice : request.choices()) {
            if (supplied.putIfAbsent(choice.requirementId(), choice.option()) != null) return reject(state, "Duplicate choice");
        }
        for (EffectChoiceRequirement requirement : request.definition().choices()) {
            String value = supplied.remove(requirement.id());
            if (value == null && requirement.required()) return reject(state, "Missing choice: " + requirement.id());
            if (value != null && !requirement.options().contains(value)) return reject(state, "Invalid choice: " + requirement.id());
        }
        if (!supplied.isEmpty()) return reject(state, "Unknown choice");
        EffectDefinition effect = request.definition();
        if (effect.condition() != EffectCondition.ALWAYS) return reject(state, "Unsupported condition");
        if (effect.timing() == EffectTiming.IMMEDIATE) return accept(state, credits(effect));
        if (effect.timing() == EffectTiming.PERMANENT) {
            List<ActivePersistentEffect> active = new ArrayList<>(state.active());
            active.add(new ActivePersistentEffect(effect, request.sourceId(), request.choices()));
            return accept(new EffectResolutionState(active, state.deferred()), List.of());
        }
        List<DeferredEffect> deferred = new ArrayList<>(state.deferred());
        deferred.add(new DeferredEffect(effect, request.sourceId(), request.choices()));
        return accept(new EffectResolutionState(state.active(), deferred), List.of());
    }

    @Override
    public EffectResolutionResult applyPersistent(EffectResolutionState state) {
        Objects.requireNonNull(state, "state");
        List<CreditGoldOperation> operations = new ArrayList<>();
        for (ActivePersistentEffect effect : state.active()) operations.addAll(credits(effect.definition()));
        return accept(state, operations);
    }

    private static List<CreditGoldOperation> credits(EffectDefinition definition) {
        return definition.operations().stream().map(CreditGoldOperation.class::cast).toList();
    }
    private static EffectResolutionResult accept(EffectResolutionState state, List<CreditGoldOperation> operations) {
        return new EffectResolutionResult(true, Optional.empty(), state, operations);
    }
    private static EffectResolutionResult reject(EffectResolutionState state, String reason) {
        return new EffectResolutionResult(false, Optional.of(reason), state, List.of());
    }
}
