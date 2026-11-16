package com.myjavacardgame.application;

import com.myjavacardgame.domain.party.ParticipantType;
import com.myjavacardgame.domain.round.*;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Drives configured solo timers, player interaction and observable single bot actions. */
public final class SoloGameDriver {
    private final SoloGameController controller;
    private final int marketSeconds;
    private final int productionSeconds;
    private final String humanId;
    private final ArrayDeque<GameSessionCommandResult> results = new ArrayDeque<>();
    private boolean paused;
    private float remainingSeconds;
    private float botDisplayDelay;
    private static final float BOT_ACTION_DISPLAY_SECONDS = 0.45f;
    private int completedTurns;
    private ActivationPrompt prompt;

    /** Creates a driver with externally configured positive turn durations. */
    public SoloGameDriver(SoloGameController controller, int marketSeconds, int productionSeconds) {
        this.controller = Objects.requireNonNull(controller, "controller");
        if (marketSeconds <= 0 || productionSeconds <= 0) {
            throw new IllegalArgumentException("Durations must be positive");
        }
        this.marketSeconds = marketSeconds;
        this.productionSeconds = productionSeconds;
        humanId = snapshot().participants().stream().filter(p -> p.type() == ParticipantType.HUMAN)
                .findFirst().orElseThrow().id();
        remainingSeconds = duration(snapshot().currentPhase());
    }

    /** Advances an unpaused session, publishing at most one automatic command per update. */
    public void update(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0) {
            throw new IllegalArgumentException("Delta must be finite and nonnegative");
        }
        if (paused || snapshot().outcome() != MatchOutcome.IN_PROGRESS) return;
        GameSessionSnapshot before = snapshot();
        RoundPhase phase = before.currentPhase();
        if (phase == RoundPhase.INCOME || phase == RoundPhase.DEPOSIT) {
            apply(new SessionPhaseCommand(phase == RoundPhase.INCOME
                    ? PhaseCommand.COMPLETE_INCOME : PhaseCommand.COMPLETE_DEPOSIT), before);
            return;
        }
        if (completedTurns == before.participants().size()) {
            apply(new SessionPhaseCommand(phase == RoundPhase.MARKET
                    ? PhaseCommand.COMPLETE_MARKET : PhaseCommand.COMPLETE_PRODUCTION), before);
            return;
        }
        remainingSeconds = Math.max(0, remainingSeconds - deltaSeconds);
        if (remainingSeconds == 0) {
            if (prompt != null) {
                GameSessionCommandResult cancelled = controller.cancelActivation(humanId);
                results.add(cancelled);
                if (!cancelled.accepted()) return;
                prompt = null;
            }
            apply(new EndTurnCommand(before.currentParticipantId()), before);
            return;
        }
        botDisplayDelay = Math.max(0, botDisplayDelay - deltaSeconds);
        if (botDisplayDelay > 0) return;
        GameSessionCommandResult result = controller.executeNextBotAction();
        if (result != null) {
            record(result, before);
            botDisplayDelay = BOT_ACTION_DISPLAY_SECONDS;
        }
    }

    /** Submits a human command; paused, finished and bot-owned turns reject without mutation. */
    public GameSessionCommandResult execute(GameSessionCommand command) {
        Objects.requireNonNull(command, "command");
        GameSessionSnapshot before = snapshot();
        if (!humanInputAllowed()) return rejectInput();
        return apply(command, before);
    }

    /** Begins a human activation without blocking the render thread. */
    public ActivationInteraction beginActivation(int baseIndex) {
        if (!humanInputAllowed()) return new ActivationInteraction.Rejected(rejectInput());
        return interaction(controller.beginActivation(humanId, baseIndex));
    }

    /** Supplies a typed reply to the current activation prompt. */
    public ActivationInteraction answerActivation(ActivationAnswer answer) {
        if (!humanInputAllowed()) return new ActivationInteraction.Rejected(rejectInput());
        return interaction(controller.answerActivation(humanId, answer));
    }

    /** Cancels the currently uncommitted activation while the game is unpaused. */
    public GameSessionCommandResult cancelActivation() {
        if (!humanInputAllowed()) return rejectInput();
        GameSessionCommandResult result = controller.cancelActivation(humanId);
        results.add(result);
        if (result.accepted()) prompt = null;
        return result;
    }

    /** Returns the current question, if an activation is awaiting a reply. */
    public Optional<ActivationPrompt> activationPrompt() { return Optional.ofNullable(prompt); }

    /** Returns the candidate's immutable project progress while leaving the committed snapshot unchanged. */
    public Optional<com.myjavacardgame.domain.project.ProjectProgressSnapshot> activationProjectProgress() {
        return controller.activationProjectProgress();
    }

    /** Returns the current effective human purchase price if the command is legal right now. */
    public Optional<MarketPurchaseQuote> quoteMarketPurchase(int marketSlot,
            com.myjavacardgame.domain.cards.CardRole role, int baseIndex) {
        if (!humanInputAllowed() || prompt != null) return Optional.empty();
        return controller.quoteMarketPurchase(humanId, marketSlot, role, baseIndex);
    }

    /** Returns the participant controlled by the player. */
    public String humanId() { return humanId; }

    /** Suspends both automatic progression and player commands. */
    public void setPaused(boolean paused) { this.paused = paused; }

    /** Returns whether session progression is paused. */
    public boolean paused() { return paused; }

    /** Returns the current queue timer; untimed phases expose zero. */
    public float remainingSeconds() { return remainingSeconds; }

    /** Returns and clears the ordered immutable command result feed. */
    public List<GameSessionCommandResult> drainResults() {
        List<GameSessionCommandResult> out = List.copyOf(results);
        results.clear();
        return out;
    }

    /** Returns the immutable committed session state. */
    public GameSessionSnapshot snapshot() { return controller.snapshot(); }

    private boolean humanInputAllowed() {
        return !paused && snapshot().outcome() == MatchOutcome.IN_PROGRESS
                && humanId.equals(snapshot().currentParticipantId());
    }

    private GameSessionCommandResult rejectInput() {
        GameSessionCommandResult result = GameSessionCommandResult.reject(
                "Player input requires an unpaused, active human turn.", snapshot());
        results.add(result);
        return result;
    }

    private ActivationInteraction interaction(ActivationInteraction interaction) {
        if (interaction instanceof ActivationInteraction.Prompt question) prompt = question.prompt();
        else if (interaction instanceof ActivationInteraction.Completed completed) {
            prompt = null;
            results.add(completed.result());
        } else if (interaction instanceof ActivationInteraction.Rejected rejected) {
            results.add(rejected.result());
            prompt = controller.pendingActivationPrompt().orElse(null);
        }
        return interaction;
    }

    private GameSessionCommandResult apply(GameSessionCommand command, GameSessionSnapshot before) {
        GameSessionCommandResult result = controller.executeHuman(command);
        record(result, before);
        return result;
    }

    private void record(GameSessionCommandResult result, GameSessionSnapshot before) {
        results.add(result);
        if (!result.accepted()) return;
        GameSessionSnapshot now = result.snapshot();
        if (now.currentPhase() != before.currentPhase() || now.roundNumber() != before.roundNumber()) {
            completedTurns = 0;
            remainingSeconds = duration(now.currentPhase());
        } else if (!now.currentParticipantId().equals(before.currentParticipantId())) {
            completedTurns++;
            remainingSeconds = duration(now.currentPhase());
        }
    }

    private int duration(RoundPhase phase) {
        return phase == RoundPhase.MARKET ? marketSeconds : phase == RoundPhase.PRODUCTION ? productionSeconds : 0;
    }
}
