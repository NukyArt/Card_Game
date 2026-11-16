package com.myjavacardgame.application;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.myjavacardgame.domain.cards.CardCatalog;
import com.myjavacardgame.domain.cards.CardDefinition;
import com.myjavacardgame.domain.cards.CardProduction;
import com.myjavacardgame.domain.cards.CardRole;
import com.myjavacardgame.domain.cards.CardRoleParameters;
import com.myjavacardgame.domain.cards.CardType;
import com.myjavacardgame.domain.cards.ProductionEffect;
import com.myjavacardgame.domain.resources.ResourceType;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Loads card definitions and their setup metadata from external JSON. */
public final class CardCatalogLoader {
    /** Creates a loader. */
    public CardCatalogLoader() { }

    /** A catalog, market quantities, starting definitions and explicitly deferred effects. */
    public record Data(CardCatalog catalog, Map<String, Integer> marketCounts,
            List<String> startingCards, Map<String, String> pendingEffects) {
        /** Defensively copies setup metadata. */
        public Data {
            Objects.requireNonNull(catalog, "catalog");
            marketCounts = Map.copyOf(marketCounts);
            startingCards = List.copyOf(startingCards);
            pendingEffects = Map.copyOf(pendingEffects);
        }
    }

    /** Reads and validates a catalog from the supplied reader without closing it. */
    public Data load(Reader input) {
        Objects.requireNonNull(input, "input");
        try {
            JsonValue root = new JsonReader().parse(input);
            JsonValue cards = require(root, "cards");
            JsonValue starting = require(root, "startingCards");
            if (!cards.isArray() || !starting.isArray()) throw new IllegalArgumentException("Expected card and starting arrays");
            Map<String, CardDefinition> definitions = new HashMap<>();
            Map<String, Integer> counts = new HashMap<>();
            Map<String, String> pending = new HashMap<>();
            for (JsonValue card = cards.child; card != null; card = card.next) {
                String id = text(card, "id");
                String name = text(card, "name");
                String type = text(card, "type");
                int count = number(card, "marketCount");
                if (count < 0) throw new IllegalArgumentException("Negative market count: " + id);
                Map<CardRole, CardRoleParameters> roles = new EnumMap<>(CardRole.class);
                roles.put(CardRole.BASE, role(require(card, "base")));
                if (card.has("upgrade")) roles.put(CardRole.UPGRADE, role(require(card, "upgrade")));
                String deferred = card.getString("pending", "");
                if (!deferred.isBlank()) {
                    String trigger = text(card, "pendingTrigger");
                    if (trigger.equals("ACTIVATION") || trigger.equals("PURCHASE")) {
                        var base = roles.get(CardRole.BASE);
                        var effects = new ArrayList<>(base.production().effects());
                        var kind = trigger.equals("ACTIVATION") ? ProductionEffect.Kind.DEFERRED : ProductionEffect.Kind.DEFERRED_PURCHASE;
                        effects.add(new ProductionEffect(kind, 0, 0, List.of()));
                        roles.put(CardRole.BASE, new CardRoleParameters(base.prices(),
                                new CardProduction(base.production().amounts(), effects)));
                    } else {
                        throw new IllegalArgumentException("Unknown pending trigger: " + trigger);
                    }
                } else if (card.has("pendingTrigger")) {
                    throw new IllegalArgumentException("Pending trigger without an effect");
                }
                CardDefinition definition = new CardDefinition(id, name, new CardType(type), roles, deferred);
                if (definitions.putIfAbsent(id, definition) != null) throw new IllegalArgumentException("Duplicate card: " + id);
                counts.put(id, count);
                if (!deferred.isBlank()) pending.put(id, deferred);
            }
            if (definitions.isEmpty()) throw new IllegalArgumentException("Catalog is empty");
            List<String> startingCards = new ArrayList<>();
            for (JsonValue item = starting.child; item != null; item = item.next) {
                String id = item.asString();
                if (!definitions.containsKey(id)) throw new IllegalArgumentException("Unknown starting card: " + id);
                startingCards.add(id);
            }
            return new Data(new CardCatalog(definitions), counts, startingCards, pending);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid card catalog", exception);
        }
    }

    private static CardRoleParameters role(JsonValue role) {
        int price = number(role, "price");
        if (price < 0) throw new IllegalArgumentException("Negative price");
        JsonValue payout = require(role, "payout");
        if (!payout.isObject()) throw new IllegalArgumentException("Payout must be an object");
        Map<ResourceType, Integer> amounts = new EnumMap<>(ResourceType.class);
        for (JsonValue item = payout.child; item != null; item = item.next) {
            int amount = integer(item, "payout");
            if (amount < 0) throw new IllegalArgumentException("Negative payout");
            if (amounts.put(ResourceType.valueOf(item.name), amount) != null) throw new IllegalArgumentException("Duplicate payout");
        }
        List<ProductionEffect> effects = new ArrayList<>();
        JsonValue entries = role.get("effects");
        if (entries != null) {
            if (!entries.isArray()) throw new IllegalArgumentException("Effects must be an array");
            for (JsonValue effect = entries.child; effect != null; effect = effect.next) {
                var kind = ProductionEffect.Kind.valueOf(text(effect, "kind"));
                int amount = number(effect, "amount");
                int cost = effect.has("cost") ? number(effect, "cost") : 0;
                JsonValue resourceList = require(effect, "resources");
                if (!resourceList.isArray()) throw new IllegalArgumentException("Resources must be an array");
                List<ResourceType> resources = new ArrayList<>();
                for (JsonValue resource = resourceList.child; resource != null; resource = resource.next) {
                    resources.add(ResourceType.valueOf(resource.asString()));
                }
                effects.add(new ProductionEffect(kind, amount, cost, resources));
            }
        }
        return new CardRoleParameters(Map.of(ResourceType.GOLD, price), new CardProduction(amounts, effects));
    }

    private static JsonValue require(JsonValue object, String key) {
        JsonValue value = object.get(key);
        if (value == null) throw new IllegalArgumentException("Missing field: " + key);
        return value;
    }

    private static String text(JsonValue object, String key) {
        JsonValue value = require(object, key);
        if (!value.isString() || value.asString().isBlank()) throw new IllegalArgumentException("Invalid text: " + key);
        return value.asString();
    }

    private static int number(JsonValue object, String key) {
        return integer(require(object, key), key);
    }

    private static int integer(JsonValue value, String key) {
        if (!value.isNumber()) throw new IllegalArgumentException("Invalid number: " + key);
        double number = value.asDouble();
        if (!Double.isFinite(number) || number != Math.rint(number)
                || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Expected integer: " + key);
        }
        return (int) number;
    }
}
