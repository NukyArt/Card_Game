package com.myjavacardgame.presentation;

import com.myjavacardgame.domain.cards.*;
import java.util.List;
import java.util.Optional;

/** Graphics-independent card content; optional roles never fabricate prices or payouts. */
public record CardViewModel(String name, String type, String description, Optional<CardRoleParameters> base,
        Optional<CardRoleParameters> upgrade, List<com.myjavacardgame.domain.effects.EffectDefinition> effects) {
    /** Projects immutable catalog data without changing gameplay or affordability. */
    public static CardViewModel from(CardDefinition definition) {
        return new CardViewModel(definition.name(), definition.type().id(), definition.domainEffectDescription(),
                Optional.ofNullable(definition.roleParameters().get(CardRole.BASE)),
                Optional.ofNullable(definition.roleParameters().get(CardRole.UPGRADE)), definition.effects());
    }
}
