package com.konzy.evo_assist.client.features.mine.blockPH;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.core.BlockPos;

public final class ServerConfirmedBlockBreaksChecks {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var now = new AtomicLong(1_000_000_000L);
        var count = new AtomicInteger();
        var tracker = new ServerConfirmedBlockBreaks(count::incrementAndGet, now::get);
        var first = new BlockPos(3, 88, 34);
        var second = new BlockPos(-2, 86, 35);

        tracker.localBreak(first);
        now.addAndGet(65_000_000L);
        check(tracker.serverUpdate(first, true) && count.get() == 1,
                "A server air update for the same recently broken position confirms a block");
        check(!tracker.serverUpdate(first, true) && count.get() == 1,
                "Repeated air updates cannot credit the same local break twice");

        tracker.localBreak(second);
        now.addAndGet(20_000_000L);
        check(!tracker.serverUpdate(second, false) && count.get() == 1,
                "A server correction to a solid block rejects a local prediction");
        now.addAndGet(20_000_000L);
        check(!tracker.serverUpdate(second, true), "A later refill cannot confirm a rejected break");

        tracker.localBreak(first);
        now.addAndGet(1_000_000_001L);
        check(!tracker.serverUpdate(first, true) && count.get() == 1,
                "A delayed server update outside the correlation window does not credit a break");

        tracker.localBreak(first);
        now.addAndGet(149_000_000L);
        check(tracker.serverUpdate(first, true) && count.get() == 2,
                "A break confirmed near the measured upper delay remains counted");

        tracker.localBreak(first);
        tracker.clear();
        check(!tracker.serverUpdate(first, true) && count.get() == 2,
                "Disconnect clears pending local predictions");

        tracker.localBreak(first);
        now.addAndGet(1_000_000_001L);
        tracker.expirePending();
        check(!tracker.serverUpdate(first, true) && count.get() == 2,
                "Expired predictions cannot be confirmed by later block refills");

        for (long delayMillis : new long[]{325, 486, 778}) {
            tracker.localBreak(first);
            now.addAndGet(delayMillis * 1_000_000L);
            int before = count.get();
            check(tracker.serverUpdate(first, true) && count.get() == before + 1,
                    "A delayed confirmation inside the one-second window is counted: " + delayMillis + "ms");
            check(!tracker.serverUpdate(first, true) && count.get() == before + 1,
                    "A delayed break is not credited twice");
        }
        tracker.localBreak(second);
        now.addAndGet(1_000_000_000L);
        int before = count.get();
        check(tracker.serverUpdate(second, true) && count.get() == before + 1,
                "A confirmation exactly at the one-second limit is counted");

        tracker.localBreak(first);
        check(!tracker.serverUpdate(second, true), "An update at another position cannot confirm a pending break");
        now.addAndGet(778_000_000L);
        check(!tracker.serverUpdate(first, false) && !tracker.serverUpdate(first, true),
                "Late server corrections still reject the prediction before a subsequent air update");
        System.out.println("Server-confirmed block break checks passed: " + checks);
    }
}
