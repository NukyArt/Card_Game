package com.myjavacardgame.domain.board;

/** Result of attempting to activate a base.
 *
 * @param board the resulting board, unchanged on failure
 * @param activated whether the base was activated
 */
public record BoardActivationResult(BoardSnapshot board, boolean activated) {
}
