package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.resources.ResourceType;
import java.util.Objects;
import java.util.Optional;

/** Typed player answers; domain validates them against its retained instruction. */
public sealed interface ActivationAnswer permits ActivationAnswer.Resource, ActivationAnswer.Gold, ActivationAnswer.Transfer {
    /** Returns the activation identifier this answer belongs to. */
    String activationId();

    /** Selects a resource or skips an optional exchange. */
    record Resource(String activationId, Optional<ResourceType> resource) implements ActivationAnswer {
        /** Requires an identifier and an explicit choice or skip. */
        public Resource {
            Objects.requireNonNull(activationId, "activationId");
            Objects.requireNonNull(resource, "resource");
        }
    }

    /** Requests a nonnegative allocation from the current gold payout. */
    record Gold(String activationId, int amount) implements ActivationAnswer {
        /** Validates input shape without interpreting the current payout. */
        public Gold {
            Objects.requireNonNull(activationId, "activationId");
            if (amount < 0) throw new IllegalArgumentException("Amount must be nonnegative");
        }
    }

    /** Selects a package transfer or declines the optional effect. */
    record Transfer(String activationId, Optional<ActivateBaseCommand.Transfer> transfer) implements ActivationAnswer {
        /** Requires an identifier and an explicit transfer or skip. */
        public Transfer {
            Objects.requireNonNull(activationId, "activationId");
            Objects.requireNonNull(transfer, "transfer");
        }
    }
}
