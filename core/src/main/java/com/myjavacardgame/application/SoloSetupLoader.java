package com.myjavacardgame.application;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.myjavacardgame.domain.party.ParticipantType;
import com.myjavacardgame.domain.party.StartingPartyDefinition;
import com.myjavacardgame.domain.party.StartingPartyParticipantSpecification;
import com.myjavacardgame.domain.project.ProjectDefinition;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Loads and validates the inputs needed to prepare a solo game. */
public final class SoloSetupLoader {
    /** Creates a solo setup loader. */
    public SoloSetupLoader() { }

    /** Validated inputs for session creation; no session has been created. */
    public record Data(CardCatalogLoader.Data cards, ProjectDefinition project,
            StartingPartyDefinition party, int roundLimit, String difficulty, int marketVisibleSlotCount,
            int marketRefreshGoldCost, int boardMaxBases, int boardMaxUpgradesPerBase, int marketSalePriceDivisor,
            int depositGoldPerBonus, int depositMaximumBonus, int roundIncomeGold, int marketTurnSeconds, int productionTurnSeconds) {
        /** Retains source compatibility for callers that do not yet consume configured rules. */
        public Data(CardCatalogLoader.Data cards, ProjectDefinition project, StartingPartyDefinition party,
                int roundLimit, String difficulty) {
            this(cards, project, party, roundLimit, difficulty, 5, 2, 6, 3, 2, 3, 5, 0, 120, 120);
        }

        /** Validates the presence of all components and configured rule values. */
        public Data {
            Objects.requireNonNull(cards, "cards");
            Objects.requireNonNull(project, "project");
            Objects.requireNonNull(party, "party");
            Objects.requireNonNull(difficulty, "difficulty");
            if (marketVisibleSlotCount <= 0 || marketRefreshGoldCost < 0 || boardMaxBases < 0
                    || boardMaxUpgradesPerBase < 0 || marketSalePriceDivisor <= 0 || depositGoldPerBonus <= 0
                    || roundIncomeGold < 0 || depositMaximumBonus < 0 || marketTurnSeconds <= 0 || productionTurnSeconds <= 0) {
                throw new IllegalArgumentException("Solo setup rule values are invalid");
            }
        }
    }

    /**
     * Loads setup, catalog and project data from supplied readers without closing them.
     *
     * @param setupReader solo setup JSON
     * @param cardReader card catalog JSON
     * @param projectReader project JSON
     * @return validated inputs for a later session builder
     * @throws IllegalArgumentException when an input or a cross-file invariant is invalid
     */
    public Data load(Reader setupReader, Reader cardReader, Reader projectReader) {
        Objects.requireNonNull(setupReader, "setupReader");
        Objects.requireNonNull(cardReader, "cardReader");
        Objects.requireNonNull(projectReader, "projectReader");
        try {
            CardCatalogLoader.Data cards = new CardCatalogLoader().load(cardReader);
            ProjectDefinition project = new ProjectCatalogLoader().load(projectReader);
            JsonValue root = new JsonReader().parse(setupReader);
            String difficulty = text(root, "difficulty");
            if (!difficulty.equals("NORMAL")) throw new IllegalArgumentException("Only NORMAL difficulty is supported");
            int roundLimit = integer(root, "roundLimit");
            int startingGold = integer(root, "startingGold");
            int marketVisibleSlotCount = integer(root, "marketVisibleSlotCount");
            int marketRefreshGoldCost = integer(root, "marketRefreshGoldCost");
            int boardMaxBases = integer(root, "boardMaxBases");
            int boardMaxUpgradesPerBase = integer(root, "boardMaxUpgradesPerBase");
            int marketSalePriceDivisor = integer(root, "marketSalePriceDivisor");
            int depositGoldPerBonus = integer(root, "depositGoldPerBonus");
            int depositMaximumBonus = integer(root, "depositMaximumBonus");
            int roundIncomeGold = integer(root, "roundIncomeGold");
            int marketTurnSeconds = integer(root, "marketTurnSeconds");
            int productionTurnSeconds = integer(root, "productionTurnSeconds");
            if (roundLimit < 1) throw new IllegalArgumentException("Round limit must be positive");
            if (startingGold < 0) throw new IllegalArgumentException("Starting gold must not be negative");
            if (cards.catalog().definitionsById().size() != 20) throw new IllegalArgumentException("Catalog must contain 20 cards");
            if (!cards.pendingEffects().isEmpty()) throw new IllegalArgumentException("Catalog contains pending effects");
            if (cards.marketCounts().values().stream().mapToLong(Integer::longValue).sum() != 34) {
                throw new IllegalArgumentException("Market deck must contain 34 cards");
            }
            List<String> startingCards = cards.startingCards();
            if (startingCards.size() != 4 || new HashSet<>(startingCards).size() != 4) {
                throw new IllegalArgumentException("Four distinct starting bases are required");
            }
            if (project.stages().size() != 5) throw new IllegalArgumentException("Project must contain five stages");
            JsonValue participants = required(root, "participants");
            if (!participants.isArray()) throw new IllegalArgumentException("Participants must be an array");
            List<StartingPartyParticipantSpecification> specifications = new ArrayList<>();
            for (JsonValue participant = participants.child; participant != null; participant = participant.next) {
                specifications.add(new StartingPartyParticipantSpecification(text(participant, "id"),
                        ParticipantType.valueOf(text(participant, "type")), startingGold, startingCards));
            }
            long humans = specifications.stream().filter(p -> p.type() == ParticipantType.HUMAN).count();
            long bots = specifications.stream().filter(p -> p.type() == ParticipantType.BOT).count();
            if (specifications.size() != 4 || humans != 1 || bots != 3
                    || specifications.get(0).type() != ParticipantType.HUMAN) {
                throw new IllegalArgumentException("Solo setup requires one human first and three bots");
            }
            StartingPartyDefinition party = StartingPartyDefinition.fromSpecifications(specifications, cards.catalog());
            return new Data(cards, project, party, roundLimit, difficulty, marketVisibleSlotCount,
                    marketRefreshGoldCost, boardMaxBases, boardMaxUpgradesPerBase, marketSalePriceDivisor,
                    depositGoldPerBonus, depositMaximumBonus, roundIncomeGold, marketTurnSeconds, productionTurnSeconds);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid solo setup", exception);
        }
    }

    private static JsonValue required(JsonValue object, String name) {
        JsonValue value = object.get(name);
        if (value == null) throw new IllegalArgumentException("Missing field: " + name);
        return value;
    }

    private static String text(JsonValue object, String name) {
        JsonValue value = required(object, name);
        if (!value.isString() || value.asString().isBlank()) throw new IllegalArgumentException("Invalid text: " + name);
        return value.asString();
    }

    private static int integer(JsonValue object, String name) {
        JsonValue value = required(object, name);
        if (!value.isNumber()) throw new IllegalArgumentException("Invalid integer: " + name);
        double number = value.asDouble();
        if (!Double.isFinite(number) || number != Math.rint(number)
                || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid integer: " + name);
        }
        return (int) number;
    }
}
