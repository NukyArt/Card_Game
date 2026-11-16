package com.myjavacardgame.domain.cards;

import com.myjavacardgame.domain.effects.EffectDefinition;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable data defining a card independently of any card instance.
 *
 * @param id stable, nonblank identifier
 * @param name nonblank display name
 * @param type data-defined card type
 * @param roleParameters parameters for each allowed role
 * @param domainEffectDescription description of the card's domain effect
 */
public record CardDefinition(
        String id,
        String name,
        CardType type,
        Map<CardRole, CardRoleParameters> roleParameters,
        String domainEffectDescription,
        List<EffectDefinition> effects) {

    /** Creates a definition without structured effects for existing callers. */
    public CardDefinition(String id, String name, CardType type,
            Map<CardRole, CardRoleParameters> roleParameters, String domainEffectDescription) {
        this(id, name, type, roleParameters, domainEffectDescription, List.of());
    }

    /**
     * Creates a card definition with an immutable copy of its role parameters.
     *
     * @param id stable, nonblank identifier
     * @param name nonblank display name
     * @param type non-null card type
     * @param roleParameters nonempty map of roles to their independent parameters
     * @param domainEffectDescription non-null domain effect description
     * @throws NullPointerException if an argument or a role map entry is null
     * @throws IllegalArgumentException if the id or name is blank, or no role is provided
     */
    public CardDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(roleParameters, "roleParameters");
        Objects.requireNonNull(domainEffectDescription, "domainEffectDescription");
        effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
        if (id.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("Card id and name must not be blank");
        }
        roleParameters = Map.copyOf(roleParameters);
        if (roleParameters.isEmpty()) {
            throw new IllegalArgumentException("A card must have at least one role");
        }
    }

    /**
     * Returns the roles for which this card has parameters.
     *
     * @return an immutable set of allowed roles
     */
    public Set<CardRole> allowedRoles() {
        return roleParameters.keySet();
    }
}
