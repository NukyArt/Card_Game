package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable questions emitted by an unpublished activation candidate. */
public sealed interface ActivationPrompt permits ActivationPrompt.ResourceChoice, ActivationPrompt.GoldAllocation,
        ActivationPrompt.TransferChoice, ActivationPrompt.RandomGoldAllocation {
    /** Returns the identifier required in the corresponding answer. */
    String activationId();

    /** A required resource selection or optional exchange, including its personal gold cost. */
    record ResourceChoice(String activationId, List<ResourceType> allowed, boolean mandatory,
            Map<ResourceType, Integer> remaining, int goldCost) implements ActivationPrompt {
        /** Creates a resource question without an exchange cost. */
        public ResourceChoice(String activationId, List<ResourceType> allowed, boolean mandatory,
                Map<ResourceType, Integer> remaining) {
            this(activationId, allowed, mandatory, remaining, 0);
        }

        /** Copies options and context so presentation cannot change the candidate. */
        public ResourceChoice {
            Objects.requireNonNull(activationId, "activationId");
            allowed = List.copyOf(allowed);
            remaining = Map.copyOf(remaining);
            if (goldCost < 0) throw new IllegalArgumentException("Exchange cost must be nonnegative");
        }
    }

    /** A known payout whose gold may be divided between the project and personal balance. */
    record GoldAllocation(String activationId, int payout, int maximumProjectGold,
            Map<ResourceType, Integer> remaining) implements ActivationPrompt {
        /** Defensively copies the current project context. */
        public GoldAllocation {
            Objects.requireNonNull(activationId, "activationId");
            remaining = Map.copyOf(remaining);
        }
    }

    /** Optional movement of a base package between different participants. */
    record TransferChoice(String activationId) implements ActivationPrompt {
        /** Requires a transaction identifier. */
        public TransferChoice { Objects.requireNonNull(activationId, "activationId"); }
    }

    /** A revealed random gold payout awaiting allocation before candidate commit. */
    record RandomGoldAllocation(String activationId, int payout, int maximumProjectGold) implements ActivationPrompt {
        /** Requires a transaction identifier. */
        public RandomGoldAllocation { Objects.requireNonNull(activationId, "activationId"); }
    }
}
