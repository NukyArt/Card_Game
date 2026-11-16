package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.board.BoardSnapshot;
import com.myjavacardgame.domain.effects.EffectResolutionState;
import com.myjavacardgame.domain.market.MarketSnapshot;
import com.myjavacardgame.domain.project.ProjectProgressSnapshot;
import com.myjavacardgame.domain.project.ProjectStageCompletedEvent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable, externally safe view of a {@link GameSession}. */
public record GameSessionSnapshot(
        List<SessionParticipantSnapshot> participants, String firstParticipantId, String currentParticipantId,
        int roundNumber, RoundPhase currentPhase, MatchOutcome outcome, Map<String, Integer> goldBalances,
        Map<String, BoardSnapshot> boards, Optional<ProjectProgressSnapshot> projectProgress,
        List<ProjectStageCompletedEvent> stageCompletedEvents, Optional<MarketSnapshot> market,
        Map<String, Boolean> soldThisMarketTurn, Map<String, Boolean> refreshedThisMarketTurn,
        Map<String, Boolean> freeRefreshEntitlements, EffectResolutionState effectState,
        List<com.myjavacardgame.domain.project.StageEffect> stageModifiers, List<TimedMarketEffect> timedMarketEffects,
        Optional<PendingRandomGold> pendingRandomGold) {
    /** Compatibility constructor for snapshots without stage and market modifiers. */
    public GameSessionSnapshot(List<SessionParticipantSnapshot> participants, String firstParticipantId,
            String currentParticipantId, int roundNumber, RoundPhase currentPhase, MatchOutcome outcome,
            Map<String, Integer> goldBalances, Map<String, BoardSnapshot> boards,
            Optional<ProjectProgressSnapshot> projectProgress, List<ProjectStageCompletedEvent> stageCompletedEvents,
            Optional<MarketSnapshot> market, Map<String, Boolean> soldThisMarketTurn,
            Map<String, Boolean> refreshedThisMarketTurn, Map<String, Boolean> freeRefreshEntitlements, EffectResolutionState effectState) {
        this(participants, firstParticipantId, currentParticipantId, roundNumber, currentPhase, outcome, goldBalances,
                boards, projectProgress, stageCompletedEvents, market, soldThisMarketTurn, refreshedThisMarketTurn,
                freeRefreshEntitlements, effectState, List.of(), List.of(), Optional.empty());
    }
    /** Compatibility constructor for snapshots without effect state. */
    public GameSessionSnapshot(List<SessionParticipantSnapshot> participants, String firstParticipantId,
            String currentParticipantId, int roundNumber, RoundPhase currentPhase, MatchOutcome outcome,
            Map<String, Integer> goldBalances, Map<String, BoardSnapshot> boards,
            Optional<ProjectProgressSnapshot> projectProgress, List<ProjectStageCompletedEvent> stageCompletedEvents,
            Optional<MarketSnapshot> market, Map<String, Boolean> soldThisMarketTurn,
            Map<String, Boolean> refreshedThisMarketTurn, Map<String, Boolean> freeRefreshEntitlements) {
        this(participants, firstParticipantId, currentParticipantId, roundNumber, currentPhase, outcome, goldBalances,
                boards, projectProgress, stageCompletedEvents, market, soldThisMarketTurn, refreshedThisMarketTurn,
                freeRefreshEntitlements, EffectResolutionState.empty());
    }
    /** Compatibility constructor for snapshots without project or market state. */
    public GameSessionSnapshot(List<SessionParticipantSnapshot> participants, String firstParticipantId,
            String currentParticipantId, int roundNumber, RoundPhase currentPhase, MatchOutcome outcome,
            Map<String, Integer> goldBalances) {
        this(participants, firstParticipantId, currentParticipantId, roundNumber, currentPhase, outcome, goldBalances,
                Map.of(), Optional.empty(), List.of(), Optional.empty(), Map.of(), Map.of(), Map.of());
    }

    /** Compatibility constructor for snapshots without market state. */
    public GameSessionSnapshot(List<SessionParticipantSnapshot> participants, String firstParticipantId,
            String currentParticipantId, int roundNumber, RoundPhase currentPhase, MatchOutcome outcome,
            Map<String, Integer> goldBalances, Map<String, BoardSnapshot> boards,
            Optional<ProjectProgressSnapshot> projectProgress, List<ProjectStageCompletedEvent> stageCompletedEvents) {
        this(participants, firstParticipantId, currentParticipantId, roundNumber, currentPhase, outcome, goldBalances,
                boards, projectProgress, stageCompletedEvents, Optional.empty(), Map.of(), Map.of(), Map.of());
    }

    /** Defensively copies all exposed collections. */
    public GameSessionSnapshot {
        Objects.requireNonNull(effectState, "effectState");
        Objects.requireNonNull(pendingRandomGold, "pendingRandomGold");
        stageModifiers = List.copyOf(Objects.requireNonNull(stageModifiers, "stageModifiers"));
        timedMarketEffects = List.copyOf(Objects.requireNonNull(timedMarketEffects, "timedMarketEffects"));
        participants = List.copyOf(Objects.requireNonNull(participants, "participants"));
        goldBalances = Map.copyOf(Objects.requireNonNull(goldBalances, "goldBalances"));
        boards = Map.copyOf(Objects.requireNonNull(boards, "boards"));
        projectProgress = Objects.requireNonNull(projectProgress, "projectProgress");
        stageCompletedEvents = List.copyOf(Objects.requireNonNull(stageCompletedEvents, "stageCompletedEvents"));
        market = Objects.requireNonNull(market, "market");
        soldThisMarketTurn = Map.copyOf(Objects.requireNonNull(soldThisMarketTurn, "soldThisMarketTurn"));
        refreshedThisMarketTurn = Map.copyOf(Objects.requireNonNull(refreshedThisMarketTurn, "refreshedThisMarketTurn"));
        freeRefreshEntitlements = Map.copyOf(Objects.requireNonNull(freeRefreshEntitlements, "freeRefreshEntitlements"));
        Objects.requireNonNull(firstParticipantId, "firstParticipantId"); Objects.requireNonNull(currentParticipantId, "currentParticipantId");
        Objects.requireNonNull(currentPhase, "currentPhase"); Objects.requireNonNull(outcome, "outcome");
        if (firstParticipantId.isBlank() || currentParticipantId.isBlank() || roundNumber < 1) throw new IllegalArgumentException("Session snapshot values are invalid");
    }
}
