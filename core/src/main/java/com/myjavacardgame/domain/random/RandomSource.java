package com.myjavacardgame.domain.random;

/**
 * Provides bounded random integers to domain code.
 *
 * <p>Implementations must return a value in the range {@code [0, bound)} and must not rely on
 * global state or system time exposed to the domain.</p>
 */
@FunctionalInterface
public interface RandomSource {
    /**
     * Returns a random integer from zero (inclusive) to {@code bound} (exclusive).
     *
     * <p>For example, {@code nextInt(3)} returns one of {@code 0}, {@code 1}, or {@code 2}.</p>
     *
     * @param bound the exclusive upper bound
     * @return a random integer in the range {@code [0, bound)}
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    int nextInt(int bound);
}
