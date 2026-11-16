package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.board.BoardOperations;
import com.myjavacardgame.domain.board.BoardPlacement;
import com.myjavacardgame.domain.board.BoardSnapshot;
import com.myjavacardgame.domain.board.BoardRules;
import com.myjavacardgame.domain.cards.CardInstance;
import com.myjavacardgame.domain.cards.CardRole;
import com.myjavacardgame.domain.cards.PlacedCard;
import com.myjavacardgame.domain.effects.CreditGoldOperation;
import com.myjavacardgame.domain.effects.DefaultEffectResolver;
import com.myjavacardgame.domain.effects.EffectResolutionRequest;
import com.myjavacardgame.domain.effects.EffectResolutionResult;
import com.myjavacardgame.domain.effects.EffectResolutionState;
import com.myjavacardgame.domain.effects.EffectResolver;
import com.myjavacardgame.domain.events.DomainEvent;
import com.myjavacardgame.domain.events.EffectResolvedEvent;
import com.myjavacardgame.domain.events.GoldCreditedEvent;
import com.myjavacardgame.domain.market.MarketDeck;
import com.myjavacardgame.domain.party.StartingPartyParticipantDefinition;
import com.myjavacardgame.domain.project.ProjectProgress;
import com.myjavacardgame.domain.project.ProjectStageCompletedEvent;
import com.myjavacardgame.domain.resources.ResourceType;
import com.myjavacardgame.domain.random.TransactionalRandomSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Clean-domain session coordinator. Implements project contribution and market rules from card-game-gdd.md §11. */
public final class GameSession {
    private final List<SessionParticipantSnapshot> participants;
    private final int roundLimit;
    private final RoundPhaseMachine phaseMachine = new RoundPhaseMachine();
    private final DepositRules depositRules;
    private final Map<String, Integer> goldBalances = new LinkedHashMap<>();
    private final Map<String, BoardSnapshot> boards = new LinkedHashMap<>();
    private final Map<String, Boolean> soldThisMarketTurn = new LinkedHashMap<>();
    private final Map<String, Boolean> refreshedThisMarketTurn = new LinkedHashMap<>();
    private final Map<String, Integer> freeRefreshMarketRounds = new LinkedHashMap<>();
    private final MarketDeck marketDeck;
    private final BoardRules boardRules;
    private ProjectProgress projectProgress;
    private final List<ProjectStageCompletedEvent> stageEvents = new ArrayList<>();
    private final List<DomainEvent> contributionEvents = new ArrayList<>();
    private final List<com.myjavacardgame.domain.project.StageEffect> stageModifiers = new ArrayList<>();
    private final List<TimedMarketEffect> timedMarketEffects = new ArrayList<>();
    private record PendingDraw(String participantId, CardActivationResolver.Pending pending) { }
    private record MarketPurchaseEvaluation(String rejectionReason, CardInstance card, int effectiveGoldCost,
            BoardSnapshot placedBoard, StageResolution stageResolution) {
        private static MarketPurchaseEvaluation reject(String reason) {
            return new MarketPurchaseEvaluation(reason, null, 0, null, null);
        }

        private boolean accepted() {
            return rejectionReason == null;
        }
    }
    private PendingDraw pendingDraw;
    private PendingActivationTransaction pendingActivation;
    private long nextActivationId;
    private final EffectResolver effectResolver;
    private EffectResolutionState effectState = EffectResolutionState.empty();
    private int firstParticipantIndex, currentParticipantIndex, completedTurnsInPhase, roundNumber = 1;
    private MatchOutcome outcome = MatchOutcome.IN_PROGRESS;

    /** Creates a session at round one in income. */
    public GameSession(GameSessionDefinition definition) {
        this(definition, new DefaultEffectResolver());
    }

    /** Creates a session with an injected effect resolver. */
    public GameSession(GameSessionDefinition definition, EffectResolver effectResolver) {
        this(definition, effectResolver, null);
    }

    /** Creates a session with explicit board limits for activation transfers. */
    public GameSession(GameSessionDefinition definition, EffectResolver effectResolver, BoardRules boardRules) {
        this.effectResolver = Objects.requireNonNull(effectResolver, "effectResolver");
        Objects.requireNonNull(definition, "definition");
        participants = definition.startingParty().participants().stream().map(GameSession::snapshotParticipant).toList();
        roundLimit = definition.roundLimit();
        depositRules = definition.depositRules();
        for (StartingPartyParticipantDefinition participant : definition.startingParty().participants()) {
            goldBalances.put(participant.id(), participant.startingGold());
            soldThisMarketTurn.put(participant.id(), false);
            refreshedThisMarketTurn.put(participant.id(), false);
        }
        boards.putAll(definition.startingBoards());
        for (Map.Entry<String, Boolean> entitlement : definition.freeRefreshEntitlements().entrySet()) {
            if (Boolean.TRUE.equals(entitlement.getValue())) {
                freeRefreshMarketRounds.put(entitlement.getKey(), roundNumber);
            }
        }
        marketDeck = definition.marketDeck().orElse(null);
        this.boardRules = boardRules != null ? boardRules : marketDeck == null ? null : marketDeck.rules().boardRules();
        projectProgress = definition.project().map(ProjectProgress::new).orElse(null);
        firstParticipantIndex = findHumanParticipantIndex(participants);
        currentParticipantIndex = firstParticipantIndex;
    }

    /**
     * Grants a participant one no-cost refresh for the next market phase.
     *
     * <p>This is a trusted integration hook for the effects subsystem, not a player command.
     * For example, an effect resolved during production grants a refresh in the following round's
     * market phase. A grant during income applies to that round's upcoming market phase; a grant
     * during market applies to the following round. A new grant replaces an unused prior grant,
     * because the rules allow at most one refresh per market turn.</p>
     *
     * @param participantId participant receiving the entitlement
     * @throws NullPointerException if {@code participantId} is null
     * @throws IllegalArgumentException if the participant is unknown or its identifier is blank
     * @throws IllegalStateException if the match has already ended
     */
    public void grantFreeRefreshForNextMarketPhase(String participantId) {
        Objects.requireNonNull(participantId, "participantId");
        if (pendingActivation != null) throw new IllegalStateException("Resolve or cancel the pending activation first.");
        if (participantId.isBlank() || !goldBalances.containsKey(participantId)) {
            throw new IllegalArgumentException("Free refresh entitlement requires a known participant");
        }
        if (outcome != MatchOutcome.IN_PROGRESS) {
            throw new IllegalStateException("Cannot grant a refresh after the match has ended");
        }
        if (pendingDraw != null) {
            throw new IllegalStateException("Resolve the pending random gold first");
        }
        if (marketDeck == null) {
            throw new IllegalStateException("Cannot grant a refresh to a session without a market");
        }
        int targetMarketRound = phaseMachine.currentPhase() == RoundPhase.INCOME ? roundNumber : roundNumber + 1;
        freeRefreshMarketRounds.put(participantId, targetMarketRound);
    }

    /** Begins a candidate activation without publishing unfinished changes. */
    public ActivationInteraction beginActivation(String participantId, int baseIndex, TransactionalRandomSource random) {
        Objects.requireNonNull(participantId, "participantId");
        Objects.requireNonNull(random, "random");
        if (pendingActivation != null) return new ActivationInteraction.Rejected(reject("An activation is already pending."));
        if (outcome != MatchOutcome.IN_PROGRESS) return new ActivationInteraction.Rejected(reject("The match has already ended."));
        if (pendingDraw != null) return new ActivationInteraction.Rejected(reject("Resolve the pending random gold first."));
        String invalid = contributionAllowed(participantId);
        if (invalid != null) return new ActivationInteraction.Rejected(reject(invalid));
        if (phaseMachine.currentPhase() != RoundPhase.PRODUCTION) {
            return new ActivationInteraction.Rejected(reject("Activation is only allowed during PRODUCTION."));
        }
        int checkpoint = random.checkpoint();
        try {
            pendingActivation = new PendingActivationTransaction("activation-" + (++nextActivationId),
                    participantId, baseIndex, roundNumber,
                    new StageResolution(projectProgress, goldBalances, stageModifiers), boards, boardRules, random);
        } catch (RuntimeException exception) {
            random.rollback(checkpoint);
            return new ActivationInteraction.Rejected(reject(activationFailureReason(exception)));
        }
        return continueActivation();
    }

    /** Answers the retained activation instruction without replaying preceding effects. */
    public ActivationInteraction answerActivation(String participantId, ActivationAnswer answer) {
        Objects.requireNonNull(participantId, "participantId");
        Objects.requireNonNull(answer, "answer");
        if (pendingActivation == null || !pendingActivation.participantId.equals(participantId)
                || !pendingActivation.id.equals(answer.activationId())) {
            return new ActivationInteraction.Rejected(reject("Activation answer does not match."));
        }
        try {
            pendingActivation.answer(answer);
        } catch (PendingActivationTransaction.InvalidAnswer exception) {
            return new ActivationInteraction.Rejected(reject(activationFailureReason(exception)));
        } catch (RuntimeException exception) {
            return abortActivation(exception);
        }
        return continueActivation();
    }

    /** Discards all unfinished changes and restores logical randomness. */
    public GameSessionCommandResult cancelActivation(String participantId) {
        Objects.requireNonNull(participantId, "participantId");
        if (pendingActivation == null || !pendingActivation.participantId.equals(participantId)) {
            return reject("No matching activation is pending.");
        }
        pendingActivation.random.rollback(pendingActivation.checkpoint);
        pendingActivation = null;
        return accept();
    }

    /** Returns an immutable project-only preview without publishing the unfinished activation. */
    public Optional<com.myjavacardgame.domain.project.ProjectProgressSnapshot> activationProjectProgress() {
        return pendingActivation == null ? Optional.empty() : Optional.of(pendingActivation.stage.progress.snapshot());
    }

    /** Returns whether an unfinished activation blocks unrelated commands. */
    public boolean hasPendingActivation() { return pendingActivation != null; }

    /** Returns the current instruction, including after an invalid answer. */
    public Optional<ActivationPrompt> pendingActivationPrompt() {
        return pendingActivation == null ? Optional.empty() : Optional.ofNullable(pendingActivation.prompt());
    }

    private ActivationInteraction continueActivation() {
        PendingActivationTransaction candidate = pendingActivation;
        try {
            ActivationPrompt prompt = candidate.advance();
            if (prompt != null) return new ActivationInteraction.Prompt(prompt);
        } catch (RuntimeException exception) {
            return abortActivation(exception);
        }
        candidate.random.commit(candidate.checkpoint);
        boards.clear();
        boards.putAll(candidate.boards);
        commitStageResolution(candidate.stage);
        timedMarketEffects.addAll(candidate.marketRights);
        if (candidate.refreshRound() != 0) freeRefreshMarketRounds.put(candidate.participantId, candidate.refreshRound());
        pendingActivation = null;
        return new ActivationInteraction.Completed(finishContribution());
    }

    private ActivationInteraction abortActivation(RuntimeException exception) {
        pendingActivation.random.rollback(pendingActivation.checkpoint);
        pendingActivation = null;
        return new ActivationInteraction.Rejected(reject(activationFailureReason(exception)));
    }

    private static String activationFailureReason(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Activation could not be completed." : message;
    }

    /** Handles one command atomically; rejected commands leave session data unchanged. */
    public GameSessionCommandResult execute(GameSessionCommand command) {
        Objects.requireNonNull(command, "command");
        if (pendingActivation != null) return reject("Resolve or cancel the pending activation first.");
        if (outcome != MatchOutcome.IN_PROGRESS) return reject("The match has already ended.");
        if (pendingDraw != null) {
            return command instanceof ResolveRandomGoldCommand c ? resolveRandomGold(c) : reject("Resolve the pending random gold first.");
        }
        if (command instanceof ResolveRandomGoldCommand) return reject("There is no pending random gold.");
        if (command instanceof ActivateBaseCommand c) return activateBase(c);
        if (command instanceof CreditGoldCommand c) return credit(c);
        if (command instanceof ContributeGoldCommand c) return contributeGold(c);
        if (command instanceof ContributeResourceCommand c) return contributeResource(c);
        if (command instanceof ContributeBoardPackageCommand c) return contributePackage(c);
        if (command instanceof BuyMarketCardCommand c) return buyMarketCard(c);
        if (command instanceof SellBasePackageCommand c) return sellBasePackage(c);
        if (command instanceof SellUpgradeCommand c) return sellUpgrade(c);
        if (command instanceof RefreshMarketCommand c) return refreshMarket(c);
        if (command instanceof EndTurnCommand c) return endTurn(c);
        if (command instanceof SessionPhaseCommand c) return phase(c);
        if (command instanceof ProjectCompletedCommand) return completeProject();
        throw new IllegalStateException("Unsupported session command: " + command.getClass().getName());
    }

    /** Returns an immutable current state. Events remain available until the next accepted contribution. */
    public GameSessionSnapshot snapshot() {
        return new GameSessionSnapshot(participants, participants.get(firstParticipantIndex).id(),
                participants.get(currentParticipantIndex).id(), roundNumber, phaseMachine.currentPhase(), outcome,
                goldBalances, boards, Optional.ofNullable(projectProgress).map(ProjectProgress::snapshot), stageEvents,
                Optional.ofNullable(marketDeck).map(MarketDeck::snapshot), soldThisMarketTurn,
                refreshedThisMarketTurn, freeRefreshEntitlementSnapshot(), effectState, stageModifiers, timedMarketEffects,
                pendingDraw == null ? Optional.empty() : Optional.of(new PendingRandomGold(pendingDraw.participantId(), 1,
                        projectProgress.remaining(ResourceType.GOLD), pendingDraw.pending().remainingDraws())));
    }

    /** Resolves a trusted effect request and commits its operations atomically. */
    public GameSessionCommandResult resolveEffect(EffectResolutionRequest request) {
        Objects.requireNonNull(request, "request");
        if (pendingActivation != null) return reject("Resolve or cancel the pending activation first.");
        if (outcome != MatchOutcome.IN_PROGRESS) return reject("The match has already ended.");
        if (pendingDraw != null) return reject("Resolve the pending random gold first.");
        EffectResolutionResult candidate = effectResolver.resolve(effectState, request);
        return commitEffect(candidate, Optional.of(new EffectResolvedEvent(request.definition().id(), request.sourceId())));
    }

    /** Applies all active persistent effects in their insertion order. */
    public GameSessionCommandResult applyPersistentEffects() {
        if (pendingActivation != null) return reject("Resolve or cancel the pending activation first.");
        if (outcome != MatchOutcome.IN_PROGRESS) return reject("The match has already ended.");
        if (pendingDraw != null) return reject("Resolve the pending random gold first.");
        return commitEffect(effectResolver.applyPersistent(effectState), Optional.empty());
    }

    private GameSessionCommandResult commitEffect(EffectResolutionResult candidate, Optional<DomainEvent> resolved) {
        if (!candidate.accepted()) return reject(candidate.rejectionReason().orElseThrow());
        Map<String, Integer> balances = new LinkedHashMap<>(goldBalances);
        List<DomainEvent> events = new ArrayList<>();
        for (CreditGoldOperation operation : candidate.operations()) {
            Integer balance = balances.get(operation.participantId());
            if (balance == null) return reject("Unknown participant: " + operation.participantId());
            long total = (long) balance + operation.amount();
            if (total > Integer.MAX_VALUE) return reject("Gold balance exceeds the supported range.");
            balances.put(operation.participantId(), (int) total);
            events.add(new GoldCreditedEvent(operation.participantId(), operation.amount()));
        }
        goldBalances.putAll(balances);
        effectState = candidate.state();
        resolved.ifPresent(events::add);
        return new GameSessionCommandResult(true, Optional.empty(), snapshot(), events);
    }

    private GameSessionCommandResult credit(CreditGoldCommand c) {
        Integer balance = goldBalances.get(c.participantId());
        if (balance == null) return reject("Unknown participant: " + c.participantId());
        if ((long) balance + c.amount() > Integer.MAX_VALUE) return reject("Gold balance exceeds the supported range.");
        goldBalances.put(c.participantId(), balance + c.amount());
        return accept();
    }

    /**
     * Returns the effective current price for a market purchase that the session would accept.
     *
     * <p>This query shares all market-purchase validation with command execution and does not mutate session state.</p>
     *
     * @param participantId participant attempting the purchase
     * @param marketSlot zero-based visible market position
     * @param role selected permitted role for the purchased card
     * @param baseIndex target base for an upgrade, or {@code -1} for a base
     * @return a quote when the corresponding purchase command is currently accepted, otherwise empty
     */
    public Optional<MarketPurchaseQuote> quoteMarketPurchase(
            String participantId, int marketSlot, CardRole role, int baseIndex) {
        MarketPurchaseEvaluation evaluation = evaluateMarketPurchase(
                new BuyMarketCardCommand(participantId, marketSlot, role, baseIndex));
        if (!evaluation.accepted()) {
            return Optional.empty();
        }
        return Optional.of(new MarketPurchaseQuote(marketSlot, role, baseIndex, evaluation.effectiveGoldCost()));
    }

    private GameSessionCommandResult buyMarketCard(BuyMarketCardCommand c) {
        MarketPurchaseEvaluation evaluation = evaluateMarketPurchase(c);
        if (!evaluation.accepted()) {
            return reject(evaluation.rejectionReason());
        }
        marketDeck.takeCard(c.marketSlot());
        boards.put(c.participantId(), evaluation.placedBoard());
        commitStageResolution(evaluation.stageResolution());
        return finishContribution();
    }

    private MarketPurchaseEvaluation evaluateMarketPurchase(BuyMarketCardCommand c) {
        String invalid = marketActionAllowed(c.participantId());
        if (invalid != null) return MarketPurchaseEvaluation.reject(invalid);
        Optional<CardInstance> marketCard = marketDeck.cardAt(c.marketSlot());
        if (marketCard.isEmpty()) return MarketPurchaseEvaluation.reject("The selected market position is empty or invalid.");
        CardInstance card = marketCard.orElseThrow();
        int price;
        try {
            price = goldPrice(card, c.role());
        } catch (IllegalArgumentException exception) {
            return MarketPurchaseEvaluation.reject(exception.getMessage());
        }
        long discount = timedMarketEffects.stream().filter(effect -> effect.kind() == TimedMarketEffect.Kind.PURCHASE_DISCOUNT
                && effect.participantId().equals(c.participantId()) && effect.round() == roundNumber
                && effect.cardType().orElseThrow().equals(card.definition().type())).mapToLong(TimedMarketEffect::amount).sum();
        if (discount > 0) price = (int) Math.max(1L, (long) price - discount);
        int balance = goldBalances.get(c.participantId());
        if (balance < price) return MarketPurchaseEvaluation.reject("The participant does not have enough gold.");
        BoardSnapshot board = boards.get(c.participantId());
        final BoardSnapshot placedBoard;
        try {
            placedBoard = c.role() == CardRole.BASE
                    ? BoardPlacement.placeBase(board, card, marketDeck.rules().boardRules())
                    : BoardPlacement.placeUpgrade(board, card, c.baseIndex(), marketDeck.rules().boardRules());
        } catch (IllegalArgumentException exception) {
            return MarketPurchaseEvaluation.reject(exception.getMessage());
        }
        StageResolution candidate = new StageResolution(projectProgress, goldBalances, stageModifiers);
        candidate.balances.put(c.participantId(), balance - price);
        try {
            for (var slot : board.slots()) {
                if (!slot.base().instance().definition().type().equals(card.definition().type())) continue;
                for (var effect : slot.base().roleParameters().production().effects()) {
                    if (effect.kind() == com.myjavacardgame.domain.cards.ProductionEffect.Kind.DEFERRED_PURCHASE) {
                        return MarketPurchaseEvaluation.reject("An applicable purchase effect is not yet supported.");
                    }
                    if (effect.kind() == com.myjavacardgame.domain.cards.ProductionEffect.Kind.MATCHING_PURCHASE_PAYOUT) {
                        if (effect.resources().size() != 1 || effect.resources().get(0) == ResourceType.GOLD || effect.cost() != 0) {
                            return MarketPurchaseEvaluation.reject("Invalid purchase production effect.");
                        }
                        candidate.contribute(effect.resources().get(0), effect.amount());
                        if (candidate.progress.isCompleted()) break;
                    }
                }
                if (candidate.progress.isCompleted()) break;
            }
        } catch (ArithmeticException exception) {
            return MarketPurchaseEvaluation.reject("Purchase effect exceeds the supported range.");
        }
        return new MarketPurchaseEvaluation(null, card, price, placedBoard, candidate);
    }

    private GameSessionCommandResult sellBasePackage(SellBasePackageCommand c) {
        String invalid = saleAllowed(c.participantId());
        if (invalid != null) return reject(invalid);
        var extraction = BoardOperations.extractBase(boards.get(c.participantId()), c.baseIndex());
        if (extraction.cardPackage().isEmpty()) return reject("The participant does not own that base.");
        List<PlacedCard> cards = new ArrayList<>();
        cards.add(extraction.cardPackage().orElseThrow().base());
        cards.addAll(extraction.cardPackage().orElseThrow().upgrades());
        return applySale(c.participantId(), extraction.board(), cards);
    }

    private GameSessionCommandResult sellUpgrade(SellUpgradeCommand c) {
        String invalid = saleAllowed(c.participantId());
        if (invalid != null) return reject(invalid);
        var extraction = BoardOperations.extractUpgrade(boards.get(c.participantId()), c.baseIndex(), c.upgradeIndex());
        if (extraction.upgrade().isEmpty()) return reject("The participant does not own that upgrade.");
        return applySale(c.participantId(), extraction.board(), List.of(extraction.upgrade().orElseThrow()));
    }

    private GameSessionCommandResult applySale(String participantId, BoardSnapshot resultingBoard, List<PlacedCard> cards) {
        int proceeds = 0;
        try {
            for (PlacedCard card : cards) {
                proceeds = Math.addExact(proceeds, goldPrice(card.instance(), card.role()) / marketDeck.rules().salePriceDivisor());
            }
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return reject("The sale price is not supported.");
        }
        int balance = goldBalances.get(participantId);
        long saleBonus = timedMarketEffects.stream().filter(effect -> effect.kind() == TimedMarketEffect.Kind.SALE_BONUS
                && effect.participantId().equals(participantId) && effect.round() == roundNumber)
                .mapToLong(TimedMarketEffect::amount).sum();
        if ((long) balance + proceeds + saleBonus > Integer.MAX_VALUE) return reject("Gold balance exceeds the supported range.");
        proceeds += (int) saleBonus;
        List<CardInstance> discardedCards = cards.stream().map(PlacedCard::instance).toList();
        boards.put(participantId, resultingBoard);
        goldBalances.put(participantId, balance + proceeds);
        marketDeck.discard(discardedCards);
        soldThisMarketTurn.put(participantId, true);
        return accept();
    }

    private GameSessionCommandResult refreshMarket(RefreshMarketCommand c) {
        String invalid = marketActionAllowed(c.participantId());
        if (invalid != null) return reject(invalid);
        if (Boolean.TRUE.equals(refreshedThisMarketTurn.get(c.participantId()))) {
            return reject("The participant has already refreshed this market turn.");
        }
        if (!marketDeck.canRefresh()) return reject("The market cannot refresh because its deck and discard are empty.");
        boolean free = freeRefreshMarketRounds.getOrDefault(c.participantId(), -1) == roundNumber;
        int balance = goldBalances.get(c.participantId());
        int cost = marketDeck.rules().refreshGoldCost();
        if (!free && balance < cost) return reject("The participant does not have enough gold to refresh.");
        if (!free) goldBalances.put(c.participantId(), balance - cost);
        else freeRefreshMarketRounds.remove(c.participantId());
        marketDeck.refresh();
        refreshedThisMarketTurn.put(c.participantId(), true);
        return accept();
    }

    private String marketActionAllowed(String participantId) {
        if (marketDeck == null) return "This session has no configured market.";
        if (!goldBalances.containsKey(participantId)) return "Unknown participant: " + participantId;
        if (phaseMachine.currentPhase() != RoundPhase.MARKET) return "Market actions are only allowed during MARKET.";
        if (completedTurnsInPhase == participants.size()) return "All participants have already completed their market turns.";
        if (!participants.get(currentParticipantIndex).id().equals(participantId)) return "It is not participant " + participantId + "'s turn.";
        return null;
    }

    private String saleAllowed(String participantId) {
        String invalid = marketActionAllowed(participantId);
        if (invalid != null) return invalid;
        return Boolean.TRUE.equals(soldThisMarketTurn.get(participantId))
                ? "The participant has already sold this market turn." : null;
    }

    private static int goldPrice(CardInstance card, CardRole role) {
        PlacedCard placed = new PlacedCard(card, role);
        for (Map.Entry<ResourceType, Integer> price : placed.roleParameters().prices().entrySet()) {
            if (price.getKey() != ResourceType.GOLD && price.getValue() != 0) {
                throw new IllegalArgumentException("Market card prices must use gold only.");
            }
        }
        return placed.roleParameters().prices().getOrDefault(ResourceType.GOLD, 0);
    }

    private GameSessionCommandResult activateBase(ActivateBaseCommand command) {
        String invalid = contributionAllowed(command.participantId());
        if (invalid != null) return reject(invalid);
        if (phaseMachine.currentPhase() != RoundPhase.PRODUCTION) return reject("Activation is only allowed during PRODUCTION.");
        BoardSnapshot board = boards.get(command.participantId());
        var activation = BoardOperations.tryActivateBase(board, command.baseIndex(), roundNumber);
        if (!activation.activated()) return reject("The base cannot be activated.");
        boolean randomEffect = board.slots().get(command.baseIndex()).base().roleParameters().production().effects().stream()
                .anyMatch(effect -> effect.kind() == com.myjavacardgame.domain.cards.ProductionEffect.Kind.RANDOM);
        if (randomEffect && command.transfer().isPresent()) return reject("Random activation does not permit transfer.");
        if (randomEffect) {
            long possibleCredits = projectProgress.maximumRemainingGoldCredit();
            if (goldBalances.values().stream().anyMatch(balance -> balance + possibleCredits > Integer.MAX_VALUE)) {
                return reject("Potential stage credit overflow during random activation.");
            }
        }
        try {
            StageResolution candidate = new StageResolution(projectProgress, goldBalances, stageModifiers);
            var result = CardActivationResolver.resolve(board, board.slots().get(command.baseIndex()), candidate,
                    command.participantId(), command.goldToProject(), command.resourceChoices(),
                    command.additionalGoldToProject(), command.exchangeChoices(), command.randomSource(), command.decisionProvider());
            Map<String, BoardSnapshot> updatedBoards = new LinkedHashMap<>(boards);
            updatedBoards.put(command.participantId(), activation.board());
            if (command.transfer().isPresent() && !result.stage().progress.isCompleted()) {
                boolean permitsTransfer = board.slots().get(command.baseIndex()).base().roleParameters().production()
                        .effects().stream().anyMatch(effect -> effect.kind() == com.myjavacardgame.domain.cards.ProductionEffect.Kind.TRANSFER);
                if (!permitsTransfer) return reject("The activated base does not permit transfers.");
                if (boardRules == null) return reject("Board limits are not configured.");
                var transfer = command.transfer().orElseThrow();
                BoardSnapshot from = updatedBoards.get(transfer.fromParticipant());
                BoardSnapshot to = updatedBoards.get(transfer.toParticipant());
                if (from == null || to == null) return reject("Unknown transfer participant.");
                var extraction = BoardOperations.extractBase(from, transfer.baseIndex());
                if (extraction.cardPackage().isEmpty()) return reject("Transfer base not found.");
                BoardSnapshot placed = BoardPlacement.placePackage(to, extraction.cardPackage().orElseThrow(), boardRules);
                updatedBoards.put(transfer.fromParticipant(), extraction.board());
                updatedBoards.put(transfer.toParticipant(), placed);
            }
            List<TimedMarketEffect> newRights = new ArrayList<>();
            boolean grantRefresh = false;
            for (var effect : result.stage().marketEffects) {
                switch (effect.kind()) {
                    case NEXT_SALE_BONUS -> newRights.add(new TimedMarketEffect(TimedMarketEffect.Kind.SALE_BONUS,
                            command.participantId(), Math.addExact(roundNumber, 1), effect.amount(), Optional.empty()));
                    case NEXT_TYPE_DISCOUNT -> newRights.add(new TimedMarketEffect(TimedMarketEffect.Kind.PURCHASE_DISCOUNT,
                            command.participantId(), Math.addExact(roundNumber, 1), effect.amount(),
                            Optional.of(board.slots().get(command.baseIndex()).base().instance().definition().type())));
                    case NEXT_FREE_REFRESH -> grantRefresh = true;
                    default -> throw new IllegalArgumentException("Unsupported market entitlement");
                }
            }
            int refreshRound = grantRefresh ? Math.addExact(roundNumber, 1) : 0;
            boards.clear();
            boards.putAll(updatedBoards);
            commitStageResolution(result.stage());
            timedMarketEffects.addAll(newRights);
            if (grantRefresh) freeRefreshMarketRounds.put(command.participantId(), refreshRound);
            pendingDraw = result.pending().map(pending -> new PendingDraw(command.participantId(), pending)).orElse(null);
            return finishContribution();
        } catch (IllegalArgumentException | ArithmeticException exception) {
            return reject(exception.getMessage());
        }
    }

    private GameSessionCommandResult resolveRandomGold(ResolveRandomGoldCommand command) {
        if (!pendingDraw.participantId().equals(command.participantId())) return reject("Only the active participant may allocate gold.");
        if (command.goldToProject() > 1) return reject("Gold allocation exceeds the revealed payout.");
        StageResolution candidate = new StageResolution(projectProgress, goldBalances, stageModifiers);
        var pending = pendingDraw.pending();
        try {
            int contributed = Math.min(command.goldToProject(), candidate.progress.remaining(ResourceType.GOLD));
            candidate.balances.put(command.participantId(), Math.addExact(candidate.balances.get(command.participantId()), 1 - contributed));
            candidate.contribute(ResourceType.GOLD, contributed);
            CardActivationResolver.Pending next = null;
            for (int left = pending.remainingDraws(); left > 0 && !candidate.progress.isCompleted(); left--) {
                int draw = pending.random().nextInt(pending.resources().size());
                if (draw < 0 || draw >= pending.resources().size()) throw new IllegalStateException("Random source returned an invalid value");
                ResourceType resource = pending.resources().get(draw);
                if (resource == ResourceType.GOLD) {
                    next = new CardActivationResolver.Pending(pending.random(), pending.resources(), left - 1);
                    break;
                }
                candidate.contribute(resource, 1);
            }
            commitStageResolution(candidate);
            pendingDraw = next == null ? null : new PendingDraw(command.participantId(), next);
            return finishContribution();
        } catch (ArithmeticException exception) {
            return reject("Random payout exceeds the supported range.");
        }
    }

    private GameSessionCommandResult contributeGold(ContributeGoldCommand c) {
        String invalid = contributionAllowed(c.participantId()); if (invalid != null) return reject(invalid);
        if (phaseMachine.currentPhase() != RoundPhase.PRODUCTION) return reject("Gold contributions are only allowed during PRODUCTION.");
        int balance = goldBalances.get(c.participantId()); if (c.amount() > balance) return reject("Gold contribution exceeds the participant balance.");
        int used = Math.min(c.amount(), projectProgress.remaining(ResourceType.GOLD));
        StageResolution candidate = new StageResolution(projectProgress, goldBalances, stageModifiers);
        candidate.balances.put(c.participantId(), balance - used);
        try {
            candidate.contribute(ResourceType.GOLD, used);
        } catch (ArithmeticException exception) {
            return reject("Stage effect exceeds the supported gold range.");
        }
        commitStageResolution(candidate);
        return finishContribution();
    }
    private GameSessionCommandResult contributeResource(ContributeResourceCommand c) {
        String invalid = contributionAllowed(c.participantId()); if (invalid != null) return reject(invalid);
        if (phaseMachine.currentPhase() != RoundPhase.PRODUCTION) return reject("Resource contributions are only allowed during PRODUCTION.");
        StageResolution candidate = new StageResolution(projectProgress, goldBalances, stageModifiers);
        try {
            candidate.contribute(c.resource(), c.amount());
        } catch (ArithmeticException exception) {
            return reject("Stage effect exceeds the supported gold range.");
        }
        commitStageResolution(candidate); return finishContribution();
    }
    private GameSessionCommandResult contributePackage(ContributeBoardPackageCommand c) {
        String invalid = contributionAllowed(c.participantId()); if (invalid != null) return reject(invalid);
        if (projectProgress.remainingCards() == 0) return reject("The current stage does not require additional cards.");
        BoardSnapshot board = boards.get(c.participantId());
        var extraction = BoardOperations.extractBase(board, c.baseIndex());
        if (extraction.cardPackage().isEmpty()) return reject("The participant does not own that base.");
        var cardPackage = extraction.cardPackage().orElseThrow();
        int packageCards = cardPackage.cardCount();
        StageResolution candidate = new StageResolution(projectProgress, goldBalances, stageModifiers);
        try {
            candidate.contributeCards(packageCards);
        } catch (ArithmeticException exception) {
            return reject("Stage effect exceeds the supported gold range.");
        }
        commitStageResolution(candidate);
        boards.put(c.participantId(), extraction.board());
        if (marketDeck != null) {
            List<CardInstance> discardedCards = new ArrayList<>();
            discardedCards.add(cardPackage.base().instance());
            for (PlacedCard upgrade : cardPackage.upgrades()) discardedCards.add(upgrade.instance());
            marketDeck.discard(discardedCards);
        }
        return finishContribution();
    }
    private String contributionAllowed(String participantId) {
        if (projectProgress == null) return "This session has no configured project.";
        if (!goldBalances.containsKey(participantId)) return "Unknown participant: " + participantId;
        if (phaseMachine.currentPhase() != RoundPhase.MARKET && phaseMachine.currentPhase() != RoundPhase.PRODUCTION) return "Contributions are only allowed during MARKET or PRODUCTION.";
        if (completedTurnsInPhase == participants.size()) return "All participants have already completed their turns.";
        if (!participants.get(currentParticipantIndex).id().equals(participantId)) return "It is not participant " + participantId + "'s turn.";
        return null;
    }
    private GameSessionCommandResult finishContribution() {
        if (projectProgress.isCompleted()) { outcome = MatchOutcome.VICTORY; phaseMachine.execute(PhaseCommand.END_MATCH); }
        return new GameSessionCommandResult(true, Optional.empty(), snapshot(), new ArrayList<>(contributionEvents));
    }
    private void commitStageResolution(StageResolution resolution) {
        projectProgress = resolution.progress;
        goldBalances.clear();
        goldBalances.putAll(resolution.balances);
        stageModifiers.clear();
        stageModifiers.addAll(resolution.modifiers);
        recordEvents(resolution.events);
        contributionEvents.clear();
        contributionEvents.addAll(resolution.domainEvents);
    }
    private void recordEvents(List<ProjectStageCompletedEvent> events) { stageEvents.clear(); stageEvents.addAll(events); }
    private GameSessionCommandResult endTurn(EndTurnCommand c) {
        RoundPhase p=phaseMachine.currentPhase(); if (p!=RoundPhase.MARKET&&p!=RoundPhase.PRODUCTION) return reject("Turns can only end during MARKET or PRODUCTION.");
        if (completedTurnsInPhase==participants.size()) return reject("All participants have already completed their " + p + " turns.");
        if (!participants.get(currentParticipantIndex).id().equals(c.participantId())) return reject("It is not participant " + c.participantId() + "'s turn.");
        completedTurnsInPhase++; currentParticipantIndex=next(currentParticipantIndex); return accept();
    }
    private GameSessionCommandResult phase(SessionPhaseCommand c) {
        if (c.phaseCommand()==PhaseCommand.END_MATCH) return reject("END_MATCH is not a session phase command.");
        if ((phaseMachine.currentPhase()==RoundPhase.MARKET||phaseMachine.currentPhase()==RoundPhase.PRODUCTION)&&completedTurnsInPhase!=participants.size()) return reject("Every participant must complete the " + phaseMachine.currentPhase()+" turn queue first.");
        if (c.phaseCommand()==PhaseCommand.COMPLETE_DEPOSIT&&phaseMachine.currentPhase()==RoundPhase.DEPOSIT) for(var e:goldBalances.entrySet()) { int bonus=Math.min(depositRules.maximumBonus(),e.getValue()/depositRules.goldPerBonus()); if ((long)e.getValue()+bonus+depositRules.roundIncomeGold()>Integer.MAX_VALUE)return reject("Deposit bonus and round income exceed the supported gold range."); }
        boolean finalDeposit = c.phaseCommand() == PhaseCommand.COMPLETE_DEPOSIT
                && phaseMachine.currentPhase() == RoundPhase.DEPOSIT && roundNumber == roundLimit;
        var result=phaseMachine.execute(finalDeposit ? PhaseCommand.END_MATCH : c.phaseCommand()); if(!result.accepted())return reject(result.rejectionReason().orElseThrow());
        if(c.phaseCommand()==PhaseCommand.COMPLETE_DEPOSIT){ for(var e:goldBalances.entrySet()) e.setValue(e.getValue()+Math.min(depositRules.maximumBonus(),e.getValue()/depositRules.goldPerBonus())+depositRules.roundIncomeGold()); if(finalDeposit){outcome=MatchOutcome.DEFEAT;}else{roundNumber++;firstParticipantIndex=next(firstParticipantIndex);resetQueue();}}
        else if(c.phaseCommand()==PhaseCommand.COMPLETE_MARKET) { expireFreeRefreshesForCompletedMarketPhase(); resetQueue(); }
        else if(phaseMachine.currentPhase()==RoundPhase.MARKET||phaseMachine.currentPhase()==RoundPhase.PRODUCTION) resetQueue(); return accept();
    }
    private GameSessionCommandResult completeProject(){ if(projectProgress!=null)return reject("A configured project can only complete through its stage contributions."); RoundPhase p=phaseMachine.currentPhase();if(p!=RoundPhase.MARKET&&p!=RoundPhase.PRODUCTION)return reject("The project can only complete during MARKET or PRODUCTION.");phaseMachine.execute(PhaseCommand.END_MATCH);outcome=MatchOutcome.VICTORY;return accept(); }
    private void expireFreeRefreshesForCompletedMarketPhase(){ freeRefreshMarketRounds.entrySet().removeIf(entry -> entry.getValue() <= roundNumber); timedMarketEffects.removeIf(effect -> effect.round() <= roundNumber); }
    private Map<String, Boolean> freeRefreshEntitlementSnapshot(){ Map<String, Boolean> snapshot = new LinkedHashMap<>(); for(String participantId:freeRefreshMarketRounds.keySet()) snapshot.put(participantId, true); return snapshot; }
    private void resetQueue(){currentParticipantIndex=firstParticipantIndex;completedTurnsInPhase=0; if(phaseMachine.currentPhase()==RoundPhase.MARKET){ for(String id:soldThisMarketTurn.keySet())soldThisMarketTurn.put(id,false); for(String id:refreshedThisMarketTurn.keySet())refreshedThisMarketTurn.put(id,false); }}
    private int next(int i){return(i+1)%participants.size();}
    private GameSessionCommandResult accept(){return GameSessionCommandResult.accept(snapshot());} private GameSessionCommandResult reject(String r){return GameSessionCommandResult.reject(r,snapshot());}
    private static SessionParticipantSnapshot snapshotParticipant(StartingPartyParticipantDefinition p){return new SessionParticipantSnapshot(p.id(),p.type());}
    private static int findHumanParticipantIndex(List<SessionParticipantSnapshot> p){for(int i=0;i<p.size();i++)if(p.get(i).type()==com.myjavacardgame.domain.party.ParticipantType.HUMAN)return i;throw new IllegalArgumentException("A game session requires a human participant");}
}
