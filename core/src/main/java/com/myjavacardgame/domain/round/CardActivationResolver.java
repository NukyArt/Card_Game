package com.myjavacardgame.domain.round;

import com.myjavacardgame.domain.board.BoardSlot;
import com.myjavacardgame.domain.board.BoardSnapshot;
import com.myjavacardgame.domain.cards.PlacedCard;
import com.myjavacardgame.domain.cards.ProductionEffect;
import com.myjavacardgame.domain.project.ProjectProgress;
import com.myjavacardgame.domain.resources.ResourceType;
import com.myjavacardgame.domain.random.RandomSource;
import java.util.Optional;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Prepares the fixed portion of an activation without mutating session state. */
final class CardActivationResolver {
    private CardActivationResolver() { }

    record Pending(RandomSource random, List<ResourceType> resources, int remainingDraws) { }
    record Result(StageResolution stage, Optional<Pending> pending) { }

    static Result resolve(BoardSnapshot board, BoardSlot slot, StageResolution stage, String participantId, int goldToProject,
            List<ResourceType> choices, List<Integer> goldChoices, List<Boolean> exchanges,
            Optional<RandomSource> randomSource) {
        return resolve(board, slot, stage, participantId, goldToProject, choices, goldChoices, exchanges,
                randomSource, Optional.empty());
    }

    static Result resolve(BoardSnapshot board, BoardSlot slot, StageResolution stage, String participantId, int goldToProject,
            List<ResourceType> choices, List<Integer> goldChoices, List<Boolean> exchanges,
            Optional<RandomSource> randomSource, Optional<ActivationDecisionProvider> decisionProvider) {
        Map<ResourceType, Integer> amounts = new EnumMap<>(ResourceType.class);
        List<PlacedCard> cards = new ArrayList<>();
        cards.add(slot.base());
        cards.addAll(slot.upgrades());
        boolean hasRandom = cards.stream().flatMap(card -> card.roleParameters().production().effects().stream())
                .anyMatch(effect -> effect.kind() == ProductionEffect.Kind.RANDOM);
        if (hasRandom) {
            long randomEffects = cards.stream().flatMap(card -> card.roleParameters().production().effects().stream())
                    .filter(effect -> effect.kind() == ProductionEffect.Kind.RANDOM).count();
            if (randomEffects != 1) throw new IllegalArgumentException("Only one random effect per activation is supported");
            long maxGold = stage.balances.get(participantId) + stage.progress.maximumRemainingGoldCredit();
            for (PlacedCard card : cards) {
                var production = card.roleParameters().production();
                maxGold += production.amounts().getOrDefault(ResourceType.GOLD, 0);
                for (var effect : production.effects()) {
                    if (effect.kind() == ProductionEffect.Kind.RANDOM) maxGold += effect.amount();
                    else throw new IllegalArgumentException("Random production cannot be combined with other effects yet");
                }
            }
            if (maxGold > Integer.MAX_VALUE) throw new IllegalArgumentException("Potential gold overflow");
        }
        int choiceIndex = 0;
        int goldIndex = 0;
        int exchangeIndex = 0;
        for (PlacedCard card : cards) {
            var production = card.roleParameters().production();
            if (production.amounts().isEmpty() && production.effects().isEmpty()) {
                throw new IllegalArgumentException("Card production is not configured");
            }
            Map<ResourceType, Integer> cardAmounts = new EnumMap<>(ResourceType.class);
            cardAmounts.putAll(production.amounts());
            for (var effect : production.effects()) {
                if (effect.kind() == ProductionEffect.Kind.DEFERRED_PURCHASE
                        || effect.kind() == ProductionEffect.Kind.MATCHING_PURCHASE_PAYOUT) continue;
                if (effect.kind() == ProductionEffect.Kind.NEXT_SALE_BONUS
                        || effect.kind() == ProductionEffect.Kind.NEXT_TYPE_DISCOUNT
                        || effect.kind() == ProductionEffect.Kind.NEXT_FREE_REFRESH) {
                    if (card != slot.base() || effect.cost() != 0 || !effect.resources().isEmpty()) {
                        throw new IllegalArgumentException("Invalid market entitlement");
                    }
                    continue;
                }
                if (effect.kind() == ProductionEffect.Kind.REPLACE_BASE) {
                    if (decisionProvider.isPresent()) {
                        choices = new ArrayList<>(choices);
                        choices.add(requireChoice(decisionProvider.orElseThrow(), effect, stage, participantId, true).orElseThrow());
                    }
                    if (card != slot.base() || effect.cost() != 0 || effect.resources().size() < 2
                            || choiceIndex >= choices.size() || !effect.resources().contains(choices.get(choiceIndex))
                            || cardAmounts.getOrDefault(effect.resources().get(0), 0) < effect.amount()) {
                        throw new IllegalArgumentException("Invalid base replacement choice");
                    }
                    ResourceType originalResource = effect.resources().get(0);
                    ResourceType replacement = choices.get(choiceIndex++);
                    cardAmounts.put(originalResource, cardAmounts.get(originalResource) - effect.amount());
                    cardAmounts.merge(replacement, effect.amount(), Math::addExact);
                    continue;
                }
                if (effect.kind() == ProductionEffect.Kind.PER_OTHER_BASE) {
                    if (card != slot.base() || effect.cost() != 0 || effect.resources().size() != 1
                            || effect.resources().get(0) == ResourceType.GOLD) {
                        throw new IllegalArgumentException("Invalid per-base production effect");
                    }
                    long matching = board.slots().stream().filter(other -> other != slot
                            && other.base().instance().definition().id().equals(card.instance().definition().id())).count();
                    amounts.merge(effect.resources().get(0), Math.multiplyExact(effect.amount(), Math.toIntExact(matching)), Math::addExact);
                    continue;
                }
                if (effect.kind() == ProductionEffect.Kind.RANDOM) {
                    if (effect.cost() != 0 || effect.resources().size() != ResourceType.values().length
                            || !new java.util.HashSet<>(effect.resources()).equals(java.util.EnumSet.allOf(ResourceType.class))) {
                        throw new IllegalArgumentException("Invalid random production configuration");
                    }
                    if (!goldChoices.isEmpty()) throw new IllegalArgumentException("Random gold must be allocated after it is revealed");
                    continue;
                }
                if (effect.kind() == ProductionEffect.Kind.TRANSFER) {
                    if (card != slot.base() || effect.amount() != 0 || effect.cost() != 0 || !effect.resources().isEmpty()) {
                        throw new IllegalArgumentException("Invalid transfer effect");
                    }
                    continue;
                }
                if (decisionProvider.isPresent() && (effect.kind() == ProductionEffect.Kind.CHOICE
                        || effect.kind() == ProductionEffect.Kind.EXCHANGE)) {
                    if (effect.kind() == ProductionEffect.Kind.CHOICE && effect.cost() != 0) {
                        throw new IllegalArgumentException("Invalid resource choice cost");
                    }
                    continue;
                }
                if (effect.kind() == ProductionEffect.Kind.EXCHANGE) {
                    if (exchangeIndex >= exchanges.size()) continue;
                    if (!exchanges.get(exchangeIndex++)) continue;
                    if (choiceIndex >= choices.size() || !effect.resources().contains(choices.get(choiceIndex))) {
                        throw new IllegalArgumentException("Invalid exchange resource");
                    }
                    ResourceType chosen = choices.get(choiceIndex++);
                    if (chosen == ResourceType.GOLD) {
                        if (goldIndex < goldChoices.size()) {
                            if (goldChoices.get(goldIndex) > effect.amount()) throw new IllegalArgumentException("Invalid additional gold allocation");
                            goldIndex++;
                        }
                    }
                    continue;
                }
                if (effect.cost() != 0) throw new IllegalArgumentException("Activation effect is not yet supported");
                ResourceType resource;
                if (effect.kind() == ProductionEffect.Kind.FIXED && effect.resources().size() == 1) {
                    resource = effect.resources().get(0);
                } else if (effect.kind() == ProductionEffect.Kind.CHOICE && choiceIndex >= choices.size()) {
                    continue;
                } else if (effect.kind() == ProductionEffect.Kind.CHOICE
                        && effect.resources().contains(choices.get(choiceIndex))) {
                    resource = choices.get(choiceIndex++);
                } else {
                    throw new IllegalArgumentException("Unsupported effect or invalid resource choice");
                }
                if (resource == ResourceType.GOLD) {
                    if (goldIndex < goldChoices.size()) {
                        if (goldChoices.get(goldIndex) > effect.amount()) throw new IllegalArgumentException("Invalid additional gold allocation");
                        goldIndex++;
                    }
                }
            }
            for (var entry : cardAmounts.entrySet()) {
                amounts.merge(entry.getKey(), entry.getValue(), Math::addExact);
            }
        }
        if (choiceIndex != choices.size()) throw new IllegalArgumentException("Unexpected resource choice");
        if (goldIndex != goldChoices.size()) throw new IllegalArgumentException("Unexpected gold allocation");
        if (exchangeIndex != exchanges.size()) throw new IllegalArgumentException("Unexpected exchange decision");
        var producingResources = java.util.EnumSet.noneOf(ResourceType.class);
        amounts.forEach((resource, amount) -> { if (amount > 0) producingResources.add(resource); });
        for (var modifier : stage.modifiers) {
            ResourceType resource = modifier.resource();
            int produced = amounts.getOrDefault(resource, 0);
            if (producingResources.contains(resource)) {
                amounts.put(resource, Math.toIntExact(Math.max(0L, (long) produced + modifier.amount())));
            }
        }
        int gold = amounts.getOrDefault(ResourceType.GOLD, 0);
        if (goldToProject > gold) throw new IllegalArgumentException("Gold allocation exceeds production");
        ProjectProgress progress = stage.progress;
        for (ResourceType resource : ResourceType.values()) {
            if (progress.isCompleted()) break;
            int amount = amounts.getOrDefault(resource, 0);
            if (resource == ResourceType.GOLD) {
                int allocation = decisionProvider.isPresent()
                        ? goldAllocation(decisionProvider.orElseThrow(), gold, stage, participantId) : goldToProject;
                int contributed = Math.min(allocation, progress.remaining(resource));
                stage.balances.put(participantId, Math.addExact(stage.balances.get(participantId), gold - contributed));
                stage.contribute(resource, contributed);
            } else if (amount > 0) {
                stage.contribute(resource, amount);
            }
        }
        choiceIndex = 0;
        goldIndex = 0;
        exchangeIndex = 0;
        for (PlacedCard card : cards) {
            for (var effect : card.roleParameters().production().effects()) {
                if (progress.isCompleted()) break;
                if (effect.kind() == ProductionEffect.Kind.RANDOM) {
                    if (randomSource.isEmpty()) throw new IllegalArgumentException("Missing random source");
                    for (int i = 0; i < effect.amount() && !progress.isCompleted(); i++) {
                        int draw = randomSource.orElseThrow().nextInt(effect.resources().size());
                        if (draw < 0 || draw >= effect.resources().size()) throw new IllegalStateException("Random source returned an invalid value");
                        ResourceType randomResource = effect.resources().get(draw);
                        if (randomResource == ResourceType.GOLD) {
                            return new Result(stage, Optional.of(new Pending(randomSource.orElseThrow(), effect.resources(), effect.amount() - i - 1)));
                        }
                        stage.contribute(randomResource, 1);
                    }
                    continue;
                }
                if (effect.kind() == ProductionEffect.Kind.NEXT_SALE_BONUS
                        || effect.kind() == ProductionEffect.Kind.NEXT_TYPE_DISCOUNT
                        || effect.kind() == ProductionEffect.Kind.NEXT_FREE_REFRESH) {
                    stage.marketEffects.add(effect);
                    continue;
                }
                if (effect.kind() == ProductionEffect.Kind.PER_OTHER_BASE || effect.kind() == ProductionEffect.Kind.TRANSFER
                        || effect.kind() == ProductionEffect.Kind.DEFERRED_PURCHASE
                        || effect.kind() == ProductionEffect.Kind.MATCHING_PURCHASE_PAYOUT) continue;
                if (effect.kind() == ProductionEffect.Kind.REPLACE_BASE) {
                    choiceIndex++;
                    continue;
                }
                ResourceType resource;
                if (decisionProvider.isPresent() && (effect.kind() == ProductionEffect.Kind.CHOICE
                        || effect.kind() == ProductionEffect.Kind.EXCHANGE)) {
                    boolean mandatory = effect.kind() == ProductionEffect.Kind.CHOICE;
                    if (!mandatory && stage.balances.get(participantId) < effect.cost()) continue;
                    Optional<ResourceType> selected = requireChoice(decisionProvider.orElseThrow(), effect,
                            stage, participantId, mandatory);
                    if (selected.isEmpty()) continue;
                    resource = selected.orElseThrow();
                    if (!mandatory) stage.balances.put(participantId, stage.balances.get(participantId) - effect.cost());
                } else {
                    if (effect.kind() == ProductionEffect.Kind.EXCHANGE) {
                        if (exchangeIndex >= exchanges.size()) throw new IllegalArgumentException("Missing exchange decision");
                        if (!exchanges.get(exchangeIndex++)) continue;
                        if (stage.balances.get(participantId) < effect.cost()) throw new IllegalArgumentException("Insufficient gold for exchange");
                        stage.balances.put(participantId, stage.balances.get(participantId) - effect.cost());
                    }
                    if ((effect.kind() == ProductionEffect.Kind.CHOICE || effect.kind() == ProductionEffect.Kind.EXCHANGE)
                            && choiceIndex >= choices.size()) throw new IllegalArgumentException("Missing resource choice");
                    resource = effect.kind() == ProductionEffect.Kind.CHOICE
                            || effect.kind() == ProductionEffect.Kind.EXCHANGE
                            ? choices.get(choiceIndex++) : effect.resources().get(0);
                }
                int amount = effect.amount();
                if (resource == ResourceType.GOLD) {
                    if (decisionProvider.isEmpty() && goldIndex >= goldChoices.size()) {
                        throw new IllegalArgumentException("Missing additional gold allocation");
                    }
                    int allocation = decisionProvider.isPresent()
                            ? goldAllocation(decisionProvider.orElseThrow(), amount, stage, participantId)
                            : goldChoices.get(goldIndex++);
                    int contributed = Math.min(allocation, progress.remaining(resource));
                    stage.balances.put(participantId, Math.addExact(stage.balances.get(participantId), amount - contributed));
                    amount = contributed;
                }
                if (amount > 0) stage.contribute(resource, amount);
            }
        }
        return new Result(stage, Optional.empty());
    }

    private static ActivationDecisionProvider.Context context(StageResolution stage, String participantId) {
        Map<ResourceType, Integer> remaining = new EnumMap<>(ResourceType.class);
        for (ResourceType resource : ResourceType.values()) {
            remaining.put(resource, stage.progress.isCompleted() ? 0 : stage.progress.remaining(resource));
        }
        return new ActivationDecisionProvider.Context(remaining, stage.balances.get(participantId));
    }

    private static Optional<ResourceType> requireChoice(ActivationDecisionProvider provider, ProductionEffect effect,
            StageResolution stage, String participantId, boolean mandatory) {
        Optional<ResourceType> selected = java.util.Objects.requireNonNull(
                provider.chooseResource(effect, context(stage, participantId), mandatory), "resource decision");
        if ((mandatory && selected.isEmpty()) || selected.filter(resource -> !effect.resources().contains(resource)).isPresent()) {
            throw new IllegalArgumentException("Invalid provided resource choice");
        }
        return selected;
    }

    private static int goldAllocation(ActivationDecisionProvider provider, int payout, StageResolution stage, String participantId) {
        int allocation = provider.goldToProject(payout, context(stage, participantId));
        if (allocation < 0 || allocation > payout) throw new IllegalArgumentException("Invalid provided gold allocation");
        return allocation;
    }
}
