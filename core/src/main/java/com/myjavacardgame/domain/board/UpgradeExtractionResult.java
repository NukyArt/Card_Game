package com.myjavacardgame.domain.board;

import com.myjavacardgame.domain.cards.PlacedCard;
import java.util.Optional;

/** Result of extracting one upgrade without removing its base.
 *
 * @param board the resulting board, unchanged on failure
 * @param upgrade the extracted upgrade, or empty for invalid indices
 */
public record UpgradeExtractionResult(BoardSnapshot board, Optional<PlacedCard> upgrade) {
}
