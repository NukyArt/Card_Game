package com.myjavacardgame.application;

import com.myjavacardgame.application.bot.MarketBotController;
import com.myjavacardgame.application.bot.ProductionBotController;
import com.myjavacardgame.domain.random.RandomSource;
import com.myjavacardgame.domain.random.TransactionalRandomSource;
import com.myjavacardgame.domain.round.ActivationAnswer;
import com.myjavacardgame.domain.round.ActivationInteraction;
import com.myjavacardgame.domain.round.MatchOutcome;
import com.myjavacardgame.domain.party.ParticipantType;
import com.myjavacardgame.domain.project.ProjectDefinition;
import com.myjavacardgame.domain.round.GameSession;
import com.myjavacardgame.domain.round.GameSessionCommand;
import com.myjavacardgame.domain.round.GameSessionCommandResult;
import com.myjavacardgame.domain.round.GameSessionSnapshot;
import com.myjavacardgame.domain.round.RoundPhase;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Application command facade for a solo session with ordered automatic bot turns.
 *
 * <p>The injected-random constructor automates market and production; the legacy constructor remains market-only.</p>
 */
public final class SoloGameController {
    private final GameSession session;
    private final MarketBotController marketBotController;
    private final ProductionBotController productionBotController;
    private final TransactionalRandomSource activationRandom;

    /**
     * Creates a legacy market-only controller around one configured solo session.
     *
     * @param session session that receives all submitted commands
     * @param project immutable project definition used by the market bot
     */
    public SoloGameController(GameSession session, ProjectDefinition project) {
        this.session = Objects.requireNonNull(session, "session");
        this.marketBotController = new MarketBotController(Objects.requireNonNull(project, "project"));
        this.productionBotController = null;
        this.activationRandom = null;
    }

    /** Creates a controller automating both market and production with an explicitly injected random source. */
    public SoloGameController(GameSession session, ProjectDefinition project, RandomSource randomSource) {
        this.session = Objects.requireNonNull(session, "session");
        this.marketBotController = new MarketBotController(Objects.requireNonNull(project, "project"));
        this.activationRandom = randomSource instanceof TransactionalRandomSource source ? source : new TransactionalRandomSource(randomSource);
        this.productionBotController = new ProductionBotController(project, activationRandom);
    }

    /**
     * Executes a command and, after acceptance, resolves consecutive current bot turns in the configured phases.
     *
     * <p>Returned results preserve execution order: the submitted command first, followed by each bot purchase
     * and production result. Bot resolution stops when a command rejects or fails to advance the session.</p>
     *
     * @param command command submitted by the session caller
     * @return immutable ordered results from the submitted command and any automatically resolved bot turns
     */
    public List<GameSessionCommandResult> execute(GameSessionCommand command) {
        Objects.requireNonNull(command, "command");
        List<GameSessionCommandResult> results = new ArrayList<>();
        GameSessionCommandResult submitted = session.execute(command);
        results.add(submitted);
        if (!submitted.accepted() || (command instanceof com.myjavacardgame.domain.round.EndTurnCommand
                && submitted.snapshot().currentParticipantId().equals(submitted.snapshot().firstParticipantId()))) {
            return List.copyOf(results);
        }
        resolveCurrentBots(results);
        return List.copyOf(results);
    }

    /** Begins a prompted human base activation. */
    public ActivationInteraction beginActivation(String participantId, int baseIndex) {
        if (activationRandom == null) throw new IllegalStateException("Activation randomness is not configured.");
        return session.beginActivation(participantId, baseIndex, activationRandom);
    }

    /** Supplies an activation prompt answer. */
    public ActivationInteraction answerActivation(String participantId, ActivationAnswer answer) { return session.answerActivation(participantId, answer); }

    /** Cancels an uncommitted activation. */
    public GameSessionCommandResult cancelActivation(String participantId) { return session.cancelActivation(participantId); }

    /** Executes exactly one submitted command without automatically resolving bots. */
    public GameSessionCommandResult executeHuman(GameSessionCommand command) {
        return session.execute(Objects.requireNonNull(command, "command"));
    }

    /** Quotes a purchase using current domain prices and placement rules without changing the session. */
    public java.util.Optional<com.myjavacardgame.domain.round.MarketPurchaseQuote> quoteMarketPurchase(
            String participantId, int marketSlot, com.myjavacardgame.domain.cards.CardRole role, int baseIndex) {
        return session.quoteMarketPurchase(participantId, marketSlot, role, baseIndex);
    }

    /** Returns the retained activation prompt after accepted or invalid answers. */
    public java.util.Optional<com.myjavacardgame.domain.round.ActivationPrompt> pendingActivationPrompt() {
        return session.pendingActivationPrompt();
    }

    /** Returns the immutable project preview of the current uncommitted activation, if any. */
    public java.util.Optional<com.myjavacardgame.domain.project.ProjectProgressSnapshot> activationProjectProgress() {
        return session.activationProjectProgress();
    }

    /** Executes at most one action for the bot that currently owns the turn. */
    public GameSessionCommandResult executeNextBotAction() {
        if (session.hasPendingActivation()) return null;
        GameSessionSnapshot snapshot = session.snapshot();
        String botId = currentBotId(snapshot);
        if (botId == null) return null;
        List<GameSessionCommandResult> results = snapshot.currentPhase() == RoundPhase.MARKET
                ? java.util.Optional.ofNullable(marketBotController.executeNextMarketAction(session, botId)).stream().toList()
                : productionBotController == null ? List.of() : java.util.Optional.ofNullable(productionBotController.executeNextProductionAction(session, botId)).stream().toList();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Returns the current immutable state of the controlled session.
     *
     * @return current session snapshot
     */
    public GameSessionSnapshot snapshot() {
        return session.snapshot();
    }

    private void resolveCurrentBots(List<GameSessionCommandResult> results) {
        java.util.Set<String> resolvedBots = new java.util.HashSet<>();
        while (true) {
            GameSessionSnapshot before = session.snapshot();
            if (before.outcome() != MatchOutcome.IN_PROGRESS || (before.currentPhase() != RoundPhase.MARKET
                    && (before.currentPhase() != RoundPhase.PRODUCTION || productionBotController == null))) {
                return;
            }
            String botId = currentBotId(before);
            if (botId == null || !resolvedBots.add(botId)) {
                return;
            }
            List<GameSessionCommandResult> botResults = before.currentPhase() == RoundPhase.MARKET
                    ? marketBotController.executeMarketTurn(session, botId)
                    : productionBotController.executeProductionTurn(session, botId);
            results.addAll(botResults);
            if (botResults.isEmpty() || botResults.stream().anyMatch(result -> !result.accepted())
                    || before.equals(session.snapshot())
                    || session.snapshot().currentParticipantId().equals(session.snapshot().firstParticipantId())) {
                return;
            }
        }
    }

    private static String currentBotId(GameSessionSnapshot snapshot) {
        return snapshot.participants().stream()
                .filter(participant -> participant.id().equals(snapshot.currentParticipantId()))
                .filter(participant -> participant.type() == ParticipantType.BOT)
                .map(participant -> participant.id())
                .findFirst()
                .orElse(null);
    }
}
