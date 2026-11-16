package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.board.*;
import com.myjavacardgame.domain.cards.PlacedCard;
import com.myjavacardgame.domain.cards.ProductionEffect;
import com.myjavacardgame.domain.random.TransactionalRandomSource;
import com.myjavacardgame.domain.resources.ResourceType;
import java.util.*;

/** Unpublished activation candidate; each answered instruction advances exactly once. */
final class PendingActivationTransaction {
    private enum State { PREPARE, MODIFIERS, FIXED, EFFECTS, RANDOM, TRANSFER, COMPLETE }
    private enum Instruction { REPLACE, RESOURCE, EXCHANGE, GOLD, TRANSFER }

    static final class InvalidAnswer extends IllegalArgumentException {
        InvalidAnswer(String message) { super(message); }
    }

    final String id;
    final String participantId;
    final StageResolution stage;
    final Map<String, BoardSnapshot> boards;
    final List<TimedMarketEffect> marketRights = new ArrayList<>();
    final TransactionalRandomSource random;
    final int checkpoint;
    private final int round;
    private final BoardRules rules;
    private final BoardSnapshot original;
    private final BoardSlot slot;
    private final List<PlacedCard> cards = new ArrayList<>();
    private final List<ProductionEffect> effects = new ArrayList<>();
    private final Map<ResourceType, Integer> amounts = new EnumMap<>(ResourceType.class);
    private State state = State.PREPARE;
    private Instruction instruction;
    private ActivationPrompt prompt;
    private ProductionEffect decisionEffect;
    private Map<ResourceType, Integer> cardAmounts;
    private int cardIndex, preparationEffectIndex, fixedResourceIndex, effectIndex;
    private int randomDrawsRemaining, goldPayout, refreshRound;
    private ProductionEffect randomEffect;
    private boolean permitsTransfer;

    PendingActivationTransaction(String id, String participantId, int baseIndex, int round,
            StageResolution stage, Map<String, BoardSnapshot> currentBoards, BoardRules rules,
            TransactionalRandomSource random) {
        this.id = id;
        this.participantId = participantId;
        this.stage = stage;
        this.boards = new LinkedHashMap<>(currentBoards);
        this.rules = rules;
        this.random = random;
        this.checkpoint = random.checkpoint();
        this.round = round;
        original = currentBoards.get(participantId);
        var activation = BoardOperations.tryActivateBase(original, baseIndex, round);
        if (!activation.activated()) throw new IllegalArgumentException("The base cannot be activated.");
        boards.put(participantId, activation.board());
        slot = original.slots().get(baseIndex);
        cards.add(slot.base());
        cards.addAll(slot.upgrades());
        validateRandomConfiguration();
    }

    private void validateRandomConfiguration() {
        long count = cards.stream().flatMap(c -> c.roleParameters().production().effects().stream())
                .filter(e -> e.kind() == ProductionEffect.Kind.RANDOM).count();
        if (count == 0) return;
        if (count != 1) throw new IllegalArgumentException("Only one random effect per activation is supported");
        long credits = stage.progress.maximumRemainingGoldCredit();
        if (stage.balances.values().stream().anyMatch(b -> (long) b + credits > Integer.MAX_VALUE)) {
            throw new IllegalArgumentException("Potential stage credit overflow during random activation.");
        }
        long maxGold = stage.balances.get(participantId) + credits;
        for (PlacedCard card : cards) {
            var production = card.roleParameters().production();
            maxGold += production.amounts().getOrDefault(ResourceType.GOLD, 0);
            for (ProductionEffect effect : production.effects()) {
                if (effect.kind() != ProductionEffect.Kind.RANDOM) {
                    throw new IllegalArgumentException("Random production cannot be combined with other effects yet");
                }
                maxGold += effect.amount();
            }
        }
        if (maxGold > Integer.MAX_VALUE) throw new IllegalArgumentException("Potential gold overflow");
    }

    ActivationPrompt advance() {
        while (prompt == null && state != State.COMPLETE) {
            switch (state) {
                case PREPARE -> prepareNextCard();
                case MODIFIERS -> { applyModifiers(); state = State.FIXED; }
                case FIXED -> applyNextFixedResource();
                case EFFECTS -> applyNextEffect();
                case RANDOM -> drawNextResource();
                case TRANSFER -> {
                    state = State.COMPLETE;
                    if (permitsTransfer && !stage.progress.isCompleted()) {
                        instruction = Instruction.TRANSFER;
                        prompt = new ActivationPrompt.TransferChoice(id);
                    }
                }
                case COMPLETE -> { }
            }
        }
        return prompt;
    }

    private void prepareNextCard() {
        if (cardIndex == cards.size()) { state = State.MODIFIERS; return; }
        PlacedCard card = cards.get(cardIndex);
        var production = card.roleParameters().production();
        if (cardAmounts == null) {
            if (production.amounts().isEmpty() && production.effects().isEmpty()) {
                throw new IllegalArgumentException("Card production is not configured");
            }
            cardAmounts = new EnumMap<>(ResourceType.class);
            cardAmounts.putAll(production.amounts());
            effects.addAll(production.effects());
            preparationEffectIndex = 0;
        }
        while (preparationEffectIndex < production.effects().size()) {
            ProductionEffect effect = production.effects().get(preparationEffectIndex++);
            switch (effect.kind()) {
                case REPLACE_BASE -> {
                    if (card != slot.base() || effect.cost() != 0 || effect.resources().size() < 2
                            || cardAmounts.getOrDefault(effect.resources().get(0), 0) < effect.amount()) {
                        throw new IllegalArgumentException("Invalid base replacement choice");
                    }
                    decisionEffect = effect;
                    instruction = Instruction.REPLACE;
                    prompt = resourcePrompt(effect, true);
                    return;
                }
                case PER_OTHER_BASE -> {
                    if (card != slot.base() || effect.cost() != 0 || effect.resources().size() != 1
                            || effect.resources().get(0) == ResourceType.GOLD) {
                        throw new IllegalArgumentException("Invalid per-base production effect");
                    }
                    long matching = original.slots().stream().filter(other -> other != slot
                            && other.base().instance().definition().id().equals(card.instance().definition().id())).count();
                    amounts.merge(effect.resources().get(0),
                            Math.multiplyExact(effect.amount(), Math.toIntExact(matching)), Math::addExact);
                }
                case RANDOM -> {
                    if (effect.cost() != 0 || effect.resources().size() != ResourceType.values().length
                            || !new HashSet<>(effect.resources()).equals(EnumSet.allOf(ResourceType.class))) {
                        throw new IllegalArgumentException("Invalid random production configuration");
                    }
                }
                case TRANSFER -> {
                    if (card != slot.base() || effect.amount() != 0 || effect.cost() != 0 || !effect.resources().isEmpty()) {
                        throw new IllegalArgumentException("Invalid transfer effect");
                    }
                    permitsTransfer = true;
                }
                case NEXT_SALE_BONUS, NEXT_TYPE_DISCOUNT, NEXT_FREE_REFRESH -> {
                    if (card != slot.base() || effect.cost() != 0 || !effect.resources().isEmpty()) {
                        throw new IllegalArgumentException("Invalid market entitlement");
                    }
                }
                case CHOICE -> {
                    if (effect.cost() != 0) throw new IllegalArgumentException("Invalid resource choice cost");
                }
                case FIXED -> {
                    if (effect.cost() != 0 || effect.resources().size() != 1) {
                        throw new IllegalArgumentException("Invalid fixed production effect");
                    }
                }
                case EXCHANGE, DEFERRED_PURCHASE, MATCHING_PURCHASE_PAYOUT -> { }
                default -> throw new IllegalArgumentException("Activation effect is not yet supported");
            }
        }
        cardAmounts.forEach((r, amount) -> amounts.merge(r, amount, Math::addExact));
        cardAmounts = null;
        cardIndex++;
    }

    private void applyModifiers() {
        var producing = EnumSet.noneOf(ResourceType.class);
        amounts.forEach((r, amount) -> { if (amount > 0) producing.add(r); });
        for (var modifier : stage.modifiers) {
            if (producing.contains(modifier.resource())) {
                amounts.put(modifier.resource(), Math.toIntExact(Math.max(0L,
                        (long) amounts.getOrDefault(modifier.resource(), 0) + modifier.amount())));
            }
        }
    }

    private void applyNextFixedResource() {
        if (stage.progress.isCompleted()) { state = State.COMPLETE; return; }
        if (fixedResourceIndex == ResourceType.values().length) { state = State.EFFECTS; return; }
        ResourceType resource = ResourceType.values()[fixedResourceIndex++];
        payout(resource, amounts.getOrDefault(resource, 0), false);
    }

    private void applyNextEffect() {
        if (stage.progress.isCompleted()) { state = State.COMPLETE; return; }
        if (effectIndex == effects.size()) { state = State.TRANSFER; return; }
        ProductionEffect effect = effects.get(effectIndex++);
        switch (effect.kind()) {
            case RANDOM -> { randomEffect = effect; randomDrawsRemaining = effect.amount(); state = State.RANDOM; }
            case NEXT_SALE_BONUS -> marketRights.add(new TimedMarketEffect(TimedMarketEffect.Kind.SALE_BONUS,
                    participantId, Math.addExact(round, 1), effect.amount(), Optional.empty()));
            case NEXT_TYPE_DISCOUNT -> marketRights.add(new TimedMarketEffect(TimedMarketEffect.Kind.PURCHASE_DISCOUNT,
                    participantId, Math.addExact(round, 1), effect.amount(), Optional.of(slot.base().instance().definition().type())));
            case NEXT_FREE_REFRESH -> refreshRound = Math.addExact(round, 1);
            case CHOICE, EXCHANGE -> {
                boolean mandatory = effect.kind() == ProductionEffect.Kind.CHOICE;
                if (!mandatory && stage.balances.get(participantId) < effect.cost()) return;
                decisionEffect = effect;
                instruction = mandatory ? Instruction.RESOURCE : Instruction.EXCHANGE;
                prompt = resourcePrompt(effect, mandatory);
            }
            case FIXED -> payout(effect.resources().get(0), effect.amount(), false);
            default -> { }
        }
    }

    private void drawNextResource() {
        if (stage.progress.isCompleted()) { state = State.COMPLETE; return; }
        if (randomDrawsRemaining == 0) { state = State.EFFECTS; return; }
        randomDrawsRemaining--;
        payout(randomEffect.resources().get(random.nextInt(randomEffect.resources().size())), 1, true);
    }

    private void payout(ResourceType resource, int amount, boolean randomPayout) {
        if (amount == 0) return;
        if (resource != ResourceType.GOLD) { stage.contribute(resource, amount); return; }
        goldPayout = amount;
        instruction = Instruction.GOLD;
        int maximum = Math.min(amount, stage.progress.remaining(ResourceType.GOLD));
        prompt = randomPayout ? new ActivationPrompt.RandomGoldAllocation(id, amount, maximum)
                : new ActivationPrompt.GoldAllocation(id, amount, maximum, remaining());
    }

    private ActivationPrompt.ResourceChoice resourcePrompt(ProductionEffect effect, boolean mandatory) {
        return new ActivationPrompt.ResourceChoice(id, effect.resources(), mandatory, remaining(), effect.cost());
    }

    private Map<ResourceType, Integer> remaining() {
        Map<ResourceType, Integer> values = new EnumMap<>(ResourceType.class);
        for (ResourceType r : ResourceType.values()) values.put(r, stage.progress.isCompleted() ? 0 : stage.progress.remaining(r));
        return values;
    }

    void answer(ActivationAnswer answer) {
        if (prompt == null || !id.equals(answer.activationId())) throw new InvalidAnswer("Activation answer does not match.");
        switch (instruction) {
            case REPLACE, RESOURCE, EXCHANGE -> {
                var choice = (ActivationPrompt.ResourceChoice) prompt;
                if (!(answer instanceof ActivationAnswer.Resource resource)
                        || (choice.mandatory() && resource.resource().isEmpty())
                        || resource.resource().filter(r -> !choice.allowed().contains(r)).isPresent()) {
                    throw new InvalidAnswer("Invalid resource answer.");
                }
            }
            case GOLD -> {
                if (!(answer instanceof ActivationAnswer.Gold gold) || gold.amount() > goldPayout) {
                    throw new InvalidAnswer("Invalid gold answer.");
                }
            }
            case TRANSFER -> {
                if (!(answer instanceof ActivationAnswer.Transfer)) throw new InvalidAnswer("Expected a transfer answer.");
            }
        }
        Instruction answered = instruction;
        if (answered == Instruction.TRANSFER) {
            applyTransfer(((ActivationAnswer.Transfer) answer).transfer());
            prompt = null;
            instruction = null;
            return;
        }
        prompt = null;
        instruction = null;
        switch (answered) {
            case REPLACE -> {
                ResourceType replacement = ((ActivationAnswer.Resource) answer).resource().orElseThrow();
                ResourceType from = decisionEffect.resources().get(0);
                cardAmounts.put(from, cardAmounts.get(from) - decisionEffect.amount());
                cardAmounts.merge(replacement, decisionEffect.amount(), Math::addExact);
            }
            case RESOURCE, EXCHANGE -> {
                Optional<ResourceType> selected = ((ActivationAnswer.Resource) answer).resource();
                if (selected.isEmpty()) return;
                if (answered == Instruction.EXCHANGE) stage.balances.put(participantId, stage.balances.get(participantId) - decisionEffect.cost());
                payout(selected.orElseThrow(), decisionEffect.amount(), false);
            }
            case GOLD -> {
                int contributed = Math.min(((ActivationAnswer.Gold) answer).amount(), stage.progress.remaining(ResourceType.GOLD));
                stage.balances.put(participantId, Math.addExact(stage.balances.get(participantId), goldPayout - contributed));
                stage.contribute(ResourceType.GOLD, contributed);
            }
            case TRANSFER -> throw new IllegalStateException("Transfer was already handled");
        }
    }

    private void applyTransfer(Optional<ActivateBaseCommand.Transfer> selected) {
        if (selected.isEmpty()) return;
        if (rules == null) throw new InvalidAnswer("Board limits are not configured.");
        var transfer = selected.orElseThrow();
        BoardSnapshot from = boards.get(transfer.fromParticipant()), to = boards.get(transfer.toParticipant());
        if (from == null || to == null) throw new InvalidAnswer("Unknown transfer participant.");
        var extraction = BoardOperations.extractBase(from, transfer.baseIndex());
        if (extraction.cardPackage().isEmpty()) throw new InvalidAnswer("Transfer base not found.");
        final BoardSnapshot placed;
        try { placed = BoardPlacement.placePackage(to, extraction.cardPackage().orElseThrow(), rules); }
        catch (IllegalArgumentException e) { throw new InvalidAnswer(e.getMessage()); }
        boards.put(transfer.fromParticipant(), extraction.board());
        boards.put(transfer.toParticipant(), placed);
    }

    ActivationPrompt prompt() { return prompt; }
    int refreshRound() { return refreshRound; }
}
