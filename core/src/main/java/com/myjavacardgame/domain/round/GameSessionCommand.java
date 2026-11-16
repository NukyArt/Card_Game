package com.myjavacardgame.domain.round;

/** A request handled by {@link GameSession#execute(GameSessionCommand)}. */
public sealed interface GameSessionCommand permits EndTurnCommand, SessionPhaseCommand, ProjectCompletedCommand,
        CreditGoldCommand, ContributeGoldCommand, ContributeResourceCommand, ContributeBoardPackageCommand,
        BuyMarketCardCommand, SellBasePackageCommand, SellUpgradeCommand, RefreshMarketCommand, ActivateBaseCommand,
        ResolveRandomGoldCommand {
}
