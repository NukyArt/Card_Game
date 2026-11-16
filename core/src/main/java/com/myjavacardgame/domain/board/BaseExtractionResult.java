package com.myjavacardgame.domain.board;

import java.util.Optional;

/** Result of extracting a base and its complete package.
 *
 * @param board the resulting board, unchanged on failure
 * @param cardPackage the extracted package, or empty for an invalid index
 */
public record BaseExtractionResult(BoardSnapshot board, Optional<BoardPackage> cardPackage) {
}
