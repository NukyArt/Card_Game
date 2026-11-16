package com.myjavacardgame.domain.random;

import java.util.Arrays;
import java.util.Objects;

/** Deterministic {@link RandomSource} for unit tests. */
final class SequenceRandomSource implements RandomSource {
    private final int[] values;
    private int nextIndex;

    SequenceRandomSource(int... values) {
        this.values = Arrays.copyOf(Objects.requireNonNull(values, "values"), values.length);
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive: " + bound);
        }
        if (nextIndex == values.length) {
            throw new IllegalStateException(
                    "SequenceRandomSource exhausted after "
                            + nextIndex
                            + " values; cannot provide a value for bound "
                            + bound);
        }

        int value = values[nextIndex++];
        if (value < 0 || value >= bound) {
            throw new IllegalStateException(
                    "SequenceRandomSource value "
                            + value
                            + " at index "
                            + (nextIndex - 1)
                            + " is outside range [0, "
                            + bound
                            + ")");
        }
        return value;
    }
}
