package com.myjavacardgame.domain.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.myjavacardgame.domain.cards.CardCatalog;
import com.myjavacardgame.domain.cards.CardDefinition;
import com.myjavacardgame.domain.cards.CardRole;
import com.myjavacardgame.domain.cards.CardRoleParameters;
import com.myjavacardgame.domain.cards.CardType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StartingPartyDefinitionTest {
    private static final int STARTING_GOLD = 10;
    private static final List<String> STARTING_BASE_IDS = List.of("treasury", "farm", "sawmill", "apprentice-tower");

    @Test
    void fromSpecificationsBuildsHumanAndThreeBotsWithConfiguredGoldAndBases() {
        // Arrange
        CardCatalog catalog = starterCatalog();
        List<StartingPartyParticipantSpecification> specifications = List.of(
                specification("human", ParticipantType.HUMAN),
                specification("bot-one", ParticipantType.BOT),
                specification("bot-two", ParticipantType.BOT),
                specification("bot-three", ParticipantType.BOT));

        // Act
        StartingPartyDefinition party = StartingPartyDefinition.fromSpecifications(specifications, catalog);

        // Assert
        assertEquals(4, party.participants().size());
        assertEquals(
                List.of(ParticipantType.HUMAN, ParticipantType.BOT, ParticipantType.BOT, ParticipantType.BOT),
                party.participants().stream().map(StartingPartyParticipantDefinition::type).toList());
        for (StartingPartyParticipantDefinition participant : party.participants()) {
            assertEquals(STARTING_GOLD, participant.startingGold());
            assertEquals(STARTING_BASE_IDS, participant.startingBaseCards().stream().map(CardDefinition::id).toList());
        }
    }

    @Test
    void fromSpecificationsCopiesInputAndExposesUnmodifiableResolvedDefinitions() {
        // Arrange
        List<StartingPartyParticipantSpecification> specifications = new ArrayList<>(
                List.of(specification("human", ParticipantType.HUMAN)));
        StartingPartyDefinition party = StartingPartyDefinition.fromSpecifications(specifications, starterCatalog());

        // Act
        specifications.clear();

        // Assert
        assertEquals(1, party.participants().size());
        assertThrows(UnsupportedOperationException.class, () -> party.participants().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> party.participants().get(0).startingBaseCards().clear());
    }

    @Test
    void fromSpecificationsRejectsDuplicateParticipantIdentifiers() {
        // Arrange
        List<StartingPartyParticipantSpecification> specifications = List.of(
                specification("participant", ParticipantType.HUMAN),
                specification("participant", ParticipantType.BOT));

        // Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> StartingPartyDefinition.fromSpecifications(specifications, starterCatalog()));
    }

    @Test
    void fromSpecificationsRejectsUnknownStartingCardIdentifier() {
        // Arrange
        StartingPartyParticipantSpecification specification = new StartingPartyParticipantSpecification(
                "human", ParticipantType.HUMAN, STARTING_GOLD, List.of("missing-card"));

        // Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> StartingPartyDefinition.fromSpecifications(List.of(specification), starterCatalog()));
    }

    @Test
    void fromSpecificationsRejectsKnownCardThatCannotServeAsABase() {
        // Arrange
        CardDefinition upgradeOnlyCard = card("upgrade-only", List.of(CardRole.UPGRADE));
        CardCatalog catalog = new CardCatalog(Map.of(upgradeOnlyCard.id(), upgradeOnlyCard));
        StartingPartyParticipantSpecification specification = new StartingPartyParticipantSpecification(
                "human", ParticipantType.HUMAN, STARTING_GOLD, List.of(upgradeOnlyCard.id()));

        // Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> StartingPartyDefinition.fromSpecifications(List.of(specification), catalog));
    }

    @Test
    void participantSpecificationRejectsNegativeStartingGold() {
        // Arrange, Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new StartingPartyParticipantSpecification(
                        "human", ParticipantType.HUMAN, -1, STARTING_BASE_IDS));
    }

    private static StartingPartyParticipantSpecification specification(String id, ParticipantType type) {
        return new StartingPartyParticipantSpecification(id, type, STARTING_GOLD, STARTING_BASE_IDS);
    }

    private static CardCatalog starterCatalog() {
        Map<String, CardDefinition> cards = new LinkedHashMap<>();
        for (String cardId : STARTING_BASE_IDS) {
            CardDefinition card = card(cardId, List.of(CardRole.BASE));
            cards.put(card.id(), card);
        }
        return new CardCatalog(cards);
    }

    private static CardDefinition card(String id, List<CardRole> roles) {
        Map<CardRole, CardRoleParameters> roleParameters = new EnumMap<>(CardRole.class);
        for (CardRole role : roles) {
            roleParameters.put(role, new CardRoleParameters(Map.of()));
        }
        return new CardDefinition(id, id, new CardType("starter"), roleParameters, "No effect");
    }
}
