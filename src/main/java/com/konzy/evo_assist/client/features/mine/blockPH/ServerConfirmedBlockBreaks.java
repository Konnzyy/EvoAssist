package com.konzy.evo_assist.client.features.mine.blockPH;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.LongSupplier;

import net.minecraft.core.BlockPos;

/** Matches a successful local break to a nearby authoritative server air update at that position. */
public final class ServerConfirmedBlockBreaks {
    private static final long CONFIRMATION_WINDOW_NANOS = 1_000_000_000L;
    private static final int MAX_PENDING = 2_048;

    private final Map<Long, ArrayDeque<Long>> pending = new HashMap<>();
    private final Runnable confirmed;
    private final LongSupplier clock;
    private int pendingCount;

    public ServerConfirmedBlockBreaks(Runnable confirmed) {
        this(confirmed, System::nanoTime);
    }

    ServerConfirmedBlockBreaks(Runnable confirmed, LongSupplier clock) {
        this.confirmed = confirmed;
        this.clock = clock;
    }

    /** Called by Fabric after the client has locally broken a block. */
    public synchronized void localBreak(BlockPos pos) {
        long now = clock.getAsLong();
        expire(now);
        while (pendingCount >= MAX_PENDING) removeOldest();
        pending.computeIfAbsent(pos.asLong(), ignored -> new ArrayDeque<>()).addLast(now);
        pendingCount++;
    }

    /** Called on the client thread after the server's block state update has been applied. */
    public synchronized boolean serverUpdate(BlockPos pos, boolean air) {
        long key = pos.asLong();
        ArrayDeque<Long> attempts = pending.get(key);
        if (attempts == null) return false;
        long now = clock.getAsLong();
        expire(now);
        attempts = pending.get(key);
        if (attempts == null || attempts.isEmpty()) return false;
        if (!air) {
            pendingCount -= attempts.size();
            pending.remove(key);
            return false;
        }
        long started = attempts.removeFirst();
        pendingCount--;
        if (attempts.isEmpty()) pending.remove(key);
        if (now - started > CONFIRMATION_WINDOW_NANOS) return false;
        confirmed.run();
        return true;
    }

    public synchronized void expirePending() { expire(clock.getAsLong()); }

    public synchronized void clear() {
        pending.clear();
        pendingCount = 0;
    }

    private void expire(long now) {
        Iterator<Map.Entry<Long, ArrayDeque<Long>>> entries = pending.entrySet().iterator();
        while (entries.hasNext()) {
            ArrayDeque<Long> attempts = entries.next().getValue();
            while (!attempts.isEmpty() && now - attempts.peekFirst() > CONFIRMATION_WINDOW_NANOS) {
                attempts.removeFirst();
                pendingCount--;
            }
            if (attempts.isEmpty()) entries.remove();
        }
    }

    private void removeOldest() {
        var iterator = pending.entrySet().iterator();
        if (!iterator.hasNext()) return;
        var entry = iterator.next();
        entry.getValue().removeFirst();
        pendingCount--;
        if (entry.getValue().isEmpty()) iterator.remove();
    }
}
