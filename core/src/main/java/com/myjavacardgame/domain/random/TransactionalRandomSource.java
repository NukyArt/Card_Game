package com.myjavacardgame.domain.random;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/** Replayable logical random stream with reversible cursor checkpoints. */
public final class TransactionalRandomSource implements RandomSource {
    private static final int CHUNK_BOUND = 1 << 30;
    private final RandomSource delegate;
    private final List<Integer> chunks = new ArrayList<>();
    private int cursor;
    /** Creates a transactional wrapper around the source used by every runtime random consumer. */
    public TransactionalRandomSource(RandomSource delegate) { this.delegate = Objects.requireNonNull(delegate); }
    /** Returns a cursor checkpoint. */ public int checkpoint() { return cursor; }
    /** Commits a checkpoint after successful transaction completion. */ public void commit(int checkpoint) { if(checkpoint<0||checkpoint>cursor) throw new IllegalArgumentException("Invalid checkpoint"); }
    /** Restores the logical cursor after cancellation. */ public void rollback(int checkpoint) { if(checkpoint<0||checkpoint>cursor) throw new IllegalArgumentException("Invalid checkpoint"); cursor=checkpoint; }
    @Override public int nextInt(int bound) {
        if(bound<=0) throw new IllegalArgumentException("bound must be positive");
        long range=1L<<60; long limit=range-(range%bound);
        long value;
        do { value=(nextChunk()<<30)|nextChunk(); } while(value>=limit);
        return (int)(value%bound);
    }
    private long nextChunk() { if(cursor<chunks.size()) return chunks.get(cursor++); int value=delegate.nextInt(CHUNK_BOUND); if(value<0||value>=CHUNK_BOUND) throw new IllegalStateException("Random source returned an invalid value"); chunks.add(value); cursor++; return value; }
}
