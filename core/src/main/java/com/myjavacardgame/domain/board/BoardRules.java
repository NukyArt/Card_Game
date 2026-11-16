package com.myjavacardgame.domain.board;

/** Limits supplied by the game rules for one player's board.
 *
 * @param maxBases maximum number of bases
 * @param maxUpgradesPerBase maximum number of upgrades under each base
 */
public record BoardRules(int maxBases, int maxUpgradesPerBase) {
    /**
     * Creates board limits.
     *
     * @param maxBases nonnegative maximum number of bases
     * @param maxUpgradesPerBase nonnegative maximum number of upgrades per base
     * @throws IllegalArgumentException if either limit is negative
     */
    public BoardRules {
        if (maxBases < 0 || maxUpgradesPerBase < 0) {
            throw new IllegalArgumentException("Board limits must not be negative");
        }
    }
}
