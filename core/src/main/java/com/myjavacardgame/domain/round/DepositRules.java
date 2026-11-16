package com.myjavacardgame.domain.round;

/** Parameters for the end-of-round deposit bonus and subsequent fixed income.
 *
 * @param goldPerBonus positive amount of remaining gold per bonus coin
 * @param maximumBonus nonnegative maximum deposit bonus per participant
 * @param roundIncomeGold nonnegative fixed income paid after the deposit calculation
 */
public record DepositRules(int goldPerBonus, int maximumBonus, int roundIncomeGold) {
    /** Retains deposit-only rules for callers that do not configure round income. */
    public DepositRules(int goldPerBonus, int maximumBonus) {
        this(goldPerBonus, maximumBonus, 0);
    }

    /**
     * Validates the externally supplied end-of-round parameters.
     *
     * @throws IllegalArgumentException if the step is not positive or a payout is negative
     */
    public DepositRules {
        if (goldPerBonus <= 0 || maximumBonus < 0 || roundIncomeGold < 0) {
            throw new IllegalArgumentException("Deposit step must be positive and payouts must not be negative");
        }
    }
}
