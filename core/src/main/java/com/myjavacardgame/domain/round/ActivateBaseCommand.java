package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.resources.ResourceType;
import com.myjavacardgame.domain.random.RandomSource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Requests activation and supplies ordered choices for additional resource payouts. */
public record ActivateBaseCommand(String participantId, int baseIndex, int goldToProject,
        List<ResourceType> resourceChoices, List<Integer> additionalGoldToProject,
        List<Boolean> exchangeChoices, Optional<Transfer> transfer, Optional<RandomSource> randomSource,
        Optional<ActivationDecisionProvider> decisionProvider) implements GameSessionCommand {
    /** Creates an activation with explicit decisions and an injected random source. */
    public ActivateBaseCommand(String participantId, int baseIndex, int goldToProject,
            List<ResourceType> resourceChoices, List<Integer> additionalGoldToProject, List<Boolean> exchangeChoices,
            Optional<Transfer> transfer, Optional<RandomSource> randomSource) {
        this(participantId, baseIndex, goldToProject, resourceChoices, additionalGoldToProject, exchangeChoices,
                transfer, randomSource, Optional.empty());
    }

    /** Creates an activation whose decisions are requested from immutable transaction contexts. */
    public ActivateBaseCommand(String participantId, int baseIndex, RandomSource randomSource,
            ActivationDecisionProvider decisionProvider) {
        this(participantId, baseIndex, 0, List.of(), List.of(), List.of(), Optional.empty(),
                Optional.of(Objects.requireNonNull(randomSource, "randomSource")),
                Optional.of(Objects.requireNonNull(decisionProvider, "decisionProvider")));
    }
    /** Creates an activation without a random source. */
    public ActivateBaseCommand(String participantId, int baseIndex, int goldToProject,
            List<ResourceType> resourceChoices, List<Integer> additionalGoldToProject, List<Boolean> exchangeChoices,
            Optional<Transfer> transfer) {
        this(participantId, baseIndex, goldToProject, resourceChoices, additionalGoldToProject, exchangeChoices, transfer, Optional.empty());
    }
    /** A requested transfer between two different participants. */
    public record Transfer(String fromParticipant, int baseIndex, String toParticipant) {
        /** Validates transfer identifiers and index. */
        public Transfer {
            Objects.requireNonNull(fromParticipant, "fromParticipant");
            Objects.requireNonNull(toParticipant, "toParticipant");
            if (fromParticipant.isBlank() || toParticipant.isBlank() || fromParticipant.equals(toParticipant) || baseIndex < 0) {
                throw new IllegalArgumentException("Invalid base transfer");
            }
        }
    }

    /** Creates an activation with exchange decisions but no transfer. */
    public ActivateBaseCommand(String participantId, int baseIndex, int goldToProject,
            List<ResourceType> resourceChoices, List<Integer> additionalGoldToProject, List<Boolean> exchangeChoices) {
        this(participantId, baseIndex, goldToProject, resourceChoices, additionalGoldToProject, exchangeChoices, Optional.empty());
    }
    /** Creates an activation with no exchange decisions. */
    public ActivateBaseCommand(String participantId, int baseIndex, int goldToProject,
            List<ResourceType> resourceChoices, List<Integer> additionalGoldToProject) {
        this(participantId, baseIndex, goldToProject, resourceChoices, additionalGoldToProject, List.of());
    }
    /** Creates an activation without additional choices. */
    public ActivateBaseCommand(String participantId, int baseIndex, int goldToProject) {
        this(participantId, baseIndex, goldToProject, List.of(), List.of());
    }

    /** Creates an activation with resource choices and no additional gold allocations. */
    public ActivateBaseCommand(String participantId, int baseIndex, int goldToProject, List<ResourceType> resourceChoices) {
        this(participantId, baseIndex, goldToProject, resourceChoices, List.of());
    }

    /** Validates and copies activation parameters. */
    public ActivateBaseCommand {
        Objects.requireNonNull(participantId, "participantId");
        resourceChoices = List.copyOf(Objects.requireNonNull(resourceChoices, "resourceChoices"));
        Objects.requireNonNull(transfer, "transfer");
        Objects.requireNonNull(randomSource, "randomSource");
        Objects.requireNonNull(decisionProvider, "decisionProvider");
        if (decisionProvider.isPresent() && (goldToProject != 0 || !resourceChoices.isEmpty()
                || !additionalGoldToProject.isEmpty() || !exchangeChoices.isEmpty() || transfer.isPresent())) {
            throw new IllegalArgumentException("Cannot combine provided and explicit activation decisions");
        }
        exchangeChoices = List.copyOf(Objects.requireNonNull(exchangeChoices, "exchangeChoices"));
        additionalGoldToProject = List.copyOf(Objects.requireNonNull(additionalGoldToProject, "additionalGoldToProject"));
        if (additionalGoldToProject.stream().anyMatch(value -> value < 0)) {
            throw new IllegalArgumentException("Additional gold allocation must not be negative");
        }
        if (participantId.isBlank() || baseIndex < 0 || goldToProject < 0) {
            throw new IllegalArgumentException("Invalid activation parameters");
        }
    }
}
