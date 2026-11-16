package com.myjavacardgame.domain.random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SequenceRandomSourceTest {
    @Test
    void nextIntReturnsZeroAndValueBelowExclusiveBound() {
        // Arrange
        RandomSource randomSource = new SequenceRandomSource(0, 2);

        // Act
        int firstValue = randomSource.nextInt(3);
        int secondValue = randomSource.nextInt(3);

        // Assert
        assertEquals(0, firstValue);
        assertEquals(2, secondValue);
    }

    @Test
    void nextIntRejectsNonPositiveBound() {
        // Arrange
        RandomSource randomSource = new SequenceRandomSource(0);

        // Act
        IllegalArgumentException zeroBoundException = assertThrows(
                IllegalArgumentException.class, () -> randomSource.nextInt(0));
        IllegalArgumentException negativeBoundException = assertThrows(
                IllegalArgumentException.class, () -> randomSource.nextInt(-1));

        // Assert
        assertEquals("bound must be positive: 0", zeroBoundException.getMessage());
        assertEquals("bound must be positive: -1", negativeBoundException.getMessage());
    }

    @Test
    void nextIntRejectsSequenceValueOutsideRequestedRange() {
        // Arrange
        RandomSource negativeValueSource = new SequenceRandomSource(-1);
        RandomSource upperBoundValueSource = new SequenceRandomSource(3);

        // Act
        IllegalStateException negativeValueException = assertThrows(
                IllegalStateException.class, () -> negativeValueSource.nextInt(3));
        IllegalStateException upperBoundValueException = assertThrows(
                IllegalStateException.class, () -> upperBoundValueSource.nextInt(3));

        // Assert
        assertTrue(negativeValueException.getMessage().contains("value -1 at index 0"));
        assertTrue(upperBoundValueException.getMessage().contains("value 3 at index 0"));
    }

    @Test
    void nextIntReportsSequenceExhaustion() {
        // Arrange
        RandomSource randomSource = new SequenceRandomSource(1);
        randomSource.nextInt(2);

        // Act
        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> randomSource.nextInt(2));

        // Assert
        assertEquals(
                "SequenceRandomSource exhausted after 1 values; cannot provide a value for bound 2",
                exception.getMessage());
    }

    @Test
    void constructorCopiesProvidedSequence() {
        // Arrange
        int[] values = {1};
        RandomSource randomSource = new SequenceRandomSource(values);
        values[0] = 0;

        // Act
        int value = randomSource.nextInt(2);

        // Assert
        assertEquals(1, value);
    }

    @Test
    void identicalSequencesProduceIdenticalResults() {
        // Arrange
        RandomSource firstSource = new SequenceRandomSource(2, 0, 1);
        RandomSource secondSource = new SequenceRandomSource(2, 0, 1);

        // Act
        int[] firstResults = {
                firstSource.nextInt(3), firstSource.nextInt(3), firstSource.nextInt(3)
        };
        int[] secondResults = {
                secondSource.nextInt(3), secondSource.nextInt(3), secondSource.nextInt(3)
        };

        // Assert
        assertArrayEquals(firstResults, secondResults);
    }
}
