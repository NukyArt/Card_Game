package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.cards.ProductionEffect;
import com.myjavacardgame.domain.resources.ResourceType;
import java.util.Map;
import java.util.Optional;

/** Supplies activation decisions against immutable, current transaction state, without owning domain rules. */
public interface ActivationDecisionProvider {
    /** Immutable remaining requirements and personal balance at the instant a decision is requested. */
    record Context(Map<ResourceType, Integer> remainingResources, int goldBalance) {
        /** Defensively copies requirements so a caller cannot mutate the activation transaction. */
        public Context {
            remainingResources = Map.copyOf(remainingResources);
        }
    }

    /** Selects an allowed resource, or skips an optional choice; mandatory choices cannot be empty. */
    Optional<ResourceType> chooseResource(ProductionEffect effect, Context context, boolean mandatory);

    /** Selects how much of the revealed gold payout should be offered to the current stage. */
    int goldToProject(int payout, Context context);
}
