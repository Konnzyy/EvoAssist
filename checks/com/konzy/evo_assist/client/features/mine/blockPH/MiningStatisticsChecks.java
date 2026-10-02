package com.konzy.evo_assist.client.features.mine.blockPH;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

public final class MiningStatisticsChecks {
    private static int checks;
    private static void check(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createDirectories(Path.of(args[0]));
        Path file = Files.createTempFile(directory, "mining-check-", ".properties");
        AtomicLong now = new AtomicLong();
        var stats = new MiningStatistics(file, error -> { throw new AssertionError(error); }, now::get);
        stats.record(10, 1000, 2);
        stats.activeSecond();
        check(stats.current().blocks == 0 && stats.current().activeMillis == 0, "Ignore events before connection");
        stats.connect(" PLAY.EXAMPLE.COM:25565 ");
        check(stats.current().blocks == 0 && stats.rates().blocks() == 0, "Empty server starts at zero");

        now.set(10_000);
        stats.record(10, 1000, 2);
        stats.activeSecond();
        check(stats.rates().blocks() == 36_000 && stats.rates().money() == 3_600_000
                && stats.rates().shards() == 7200, "Initial estimate works without waiting five minutes or dividing by zero");
        now.set(12_000);
        check(stats.rates().blocks() == 18_000, "Initial denominator includes elapsed idle time");
        stats.record(10, 1000, 2);
        now.set(70_000);
        check(stats.rates().blocks() == 1200 && stats.rates().money() == 120_000
                && stats.rates().shards() == 240, "Estimate includes one minute of wall time");
        now.set(310_000);
        check(stats.rates().blocks() == 120 && stats.rates().shards() == 24, "Expire only earnings older than five minutes");
        now.set(312_000);
        check(stats.rates().blocks() == 0 && stats.rates().money() == 0 && stats.rates().shards() == 0,
                "Idle rates reach zero after five minutes");
        check(stats.current().blocks == 20 && stats.current().money == 2000
                && stats.current().shards == 4 && stats.current().activeMillis == 1000, "Aging rates never changes totals or active time");

        stats.reset();
        for (int second = 0; second <= 300; second++) {
            now.set(400_000 + second * 1000L);
            stats.record(1, 100, 1);
            stats.activeSecond();
        }
        check(stats.rates().blocks() == 3600 && stats.rates().money() == 360_000 && stats.rates().shards() == 3600,
                "Steady five-minute rate ignores old events");
        now.set(850_000);
        check(stats.rates().blocks() == 1800 && stats.rates().money() == 180_000 && stats.rates().shards() == 1800,
                "Steady rate falls by half after half a window idle");
        now.set(1_000_000);
        check(stats.rates().blocks() == 0, "Steady rate reaches zero after a full idle window");
        check(stats.current().blocks == 301 && stats.current().activeMillis == 301_000, "Active time is independent of rolling window");

        stats.reset();
        now.set(1_100_000);
        stats.record(10, 500, 3);
        stats.activeSecond();
        now.set(1_110_000);
        stats.disconnect();
        stats.record(100, 100, 100);
        stats.activeSecond();
        check(stats.current().blocks == 10 && stats.current().activeMillis == 1000, "Disconnected events do not change totals");
        now.set(1_120_000);
        stats.connect("play.example.com");
        check(stats.current().blocks == 10 && stats.current().money == 500 && stats.current().shards == 3,
                "Same server restores totals across reconnect and normalizes the default port");
        check(stats.rates().blocks() == 1800, "Reconnect preserves recent window and includes disconnected time");
        stats.connect("other.example.com");
        check(stats.current().blocks == 0 && stats.rates().blocks() == 0, "Another server has independent totals and rates");
        stats.record(7, 77, 1);
        stats.activeSecond();
        stats.connect("play.example.com");
        check(stats.current().blocks == 10 && stats.rates().blocks() == 1800, "Returning to first server restores its own window");
        stats.reset();
        check(stats.current().blocks == 0 && stats.current().money == 0 && stats.current().shards == 0
                && stats.current().activeMillis == 0 && stats.rates().blocks() == 0, "Explicit reset clears totals and derived rate together");
        stats.connect("other.example.com");
        check(stats.current().blocks == 7 && stats.current().money == 77, "Reset affects only the selected server");
        stats.connect("other.example.com:25566");
        check(stats.current().blocks == 0, "A different non-default port is a different server");
        stats.record(2, 20, 2);
        stats.disconnect();

        stats = new MiningStatistics(file, error -> { throw new AssertionError(error); }, now::get);
        check(stats.current().blocks == 2 && !stats.isConnected(), "Totals survive process restart without enabling disconnected tracking");
        check(stats.rates().blocks() == 0, "Restart does not treat stored totals as recent earnings");
        stats.connect("other.example.com");
        check(stats.current().blocks == 7 && stats.current().money == 77 && stats.current().shards == 1
                && stats.current().activeMillis == 1000, "All fields survive disk reload per server");
        stats.connect("play.example.com");
        check(stats.current().blocks == 0, "Explicit reset survives disk reload");
        for (int i = 0; i < 10_000; i++) stats.record(1, 0, 0);
        now.addAndGet(5000);
        stats.tick();
        var loaded = new MiningStatistics(file, error -> { throw new AssertionError(error); }, now::get);
        check(loaded.current().blocks == 10_000, "Periodic save persists bursts without requiring disconnect");
        check(stats.rates().blocks() == 7_200_000, "Many events in one second retain their full contribution");
        stats.record(0, Long.MAX_VALUE, 0);
        stats.record(0, 1, 0);
        check(stats.current().money == Long.MAX_VALUE && stats.rates().money() > 0, "Large cumulative earnings never wrap negative");
        stats.reset();
        stats.connect("other.example.com");
        stats.connect("other.example.com");
        check(stats.current().blocks == 7 && stats.current().activeMillis == 1000,
                "Repeated JOIN during spawn transitions never resets the same server");
        Files.deleteIfExists(file);
        System.out.println("Mining statistics checks passed: " + checks);
    }
}
