package com.konzy.evo_assist.client.features.mine.blockPH;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Persistent totals by server, with a separate rolling estimate of recent mining speed. */
public final class MiningStatistics {
    private static final long WINDOW_MS = 300_000;
    private static final long SAVE_INTERVAL_MS = 5_000;

    public static final class Totals {
        public long blocks, money, shards, activeMillis;
    }

    public record Rates(long blocks, long money, long shards) {}

    private static final class Sample {
        final long at;
        long blocks, money, shards;
        Sample(long at) { this.at = at; }
    }

    private static final class Window {
        final ArrayDeque<Sample> samples = new ArrayDeque<>();
        long startedAt;
        boolean started;

        void trim(long now) {
            while (!samples.isEmpty() && samples.getFirst().at <= now - WINDOW_MS) samples.removeFirst();
        }

        void record(long now, long blocks, long money, long shards) {
            trim(now);
            if (!started) {
                started = true;
                startedAt = now;
            }
            // Aggregate events in one-second buckets instead of retaining every broken block.
            long bucket = Math.floorDiv(now, 1000) * 1000;
            Sample sample = samples.peekLast();
            if (sample == null || sample.at != bucket) {
                sample = new Sample(bucket);
                samples.addLast(sample);
            }
            sample.blocks = add(sample.blocks, blocks);
            sample.money = add(sample.money, money);
            sample.shards = add(sample.shards, shards);
        }

        Rates rates(long now) {
            trim(now);
            if (samples.isEmpty()) return new Rates(0, 0, 0);
            long blocks = 0, money = 0, shards = 0;
            for (Sample sample : samples) {
                blocks = add(blocks, sample.blocks);
                money = add(money, sample.money);
                shards = add(shards, sample.shards);
            }
            long elapsed = Math.clamp(now - startedAt, 1000, WINDOW_MS);
            return new Rates(perHour(blocks, elapsed), perHour(money, elapsed), perHour(shards, elapsed));
        }
    }

    private final Path file;
    private final Consumer<Exception> reportError;
    private final LongSupplier clock;
    private final Map<String, Totals> servers = new HashMap<>();
    private final Map<String, Window> windows = new HashMap<>();
    private String currentServer = "";
    private boolean connected, dirty;
    private long lastSavedAt;

    public MiningStatistics(Path file, Consumer<Exception> reportError) {
        this(file, reportError, () -> System.nanoTime() / 1_000_000);
    }

    public MiningStatistics(Path file, Consumer<Exception> reportError, LongSupplier clock) {
        this.file = file;
        this.reportError = reportError;
        this.clock = clock;
        lastSavedAt = clock.getAsLong();
        load();
    }

    public Totals current() { return servers.computeIfAbsent(currentServer, _ -> new Totals()); }
    public boolean isConnected() { return connected; }

    public void connect(String address) {
        save();
        currentServer = address.strip().toLowerCase(Locale.ROOT);
        if (currentServer.endsWith(":25565")) currentServer = currentServer.substring(0, currentServer.length() - 6);
        connected = !currentServer.isEmpty();
        current();
        dirty = true;
        save();
    }

    public void disconnect() {
        save();
        connected = false;
    }

    public void record(long blocks, long money, long shards) {
        if (!connected) return;
        if (blocks < 0 || money < 0 || shards < 0) throw new IllegalArgumentException("Negative mining reward");
        if (blocks == 0 && money == 0 && shards == 0) return;
        Totals totals = current();
        totals.blocks = add(totals.blocks, blocks);
        totals.money = add(totals.money, money);
        totals.shards = add(totals.shards, shards);
        windows.computeIfAbsent(currentServer, _ -> new Window()).record(clock.getAsLong(), blocks, money, shards);
        dirty = true;
    }

    public void activeSecond() {
        if (!connected) return;
        current().activeMillis = add(current().activeMillis, 1000);
        dirty = true;
    }

    public Rates rates() {
        Window window = windows.get(currentServer);
        return window == null ? new Rates(0, 0, 0) : window.rates(clock.getAsLong());
    }

    public void tick() {
        if (dirty && clock.getAsLong() - lastSavedAt >= SAVE_INTERVAL_MS) save();
    }

    public void reset() {
        servers.put(currentServer, new Totals());
        windows.remove(currentServer);
        dirty = true;
        save();
    }

    public void save() {
        if (!dirty) return;
        Properties values = new Properties();
        values.setProperty("lastServer", currentServer);
        for (var entry : servers.entrySet()) {
            if (entry.getKey().isEmpty()) continue;
            String prefix = "server." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(entry.getKey().getBytes(StandardCharsets.UTF_8)) + ".";
            Totals totals = entry.getValue();
            values.setProperty(prefix + "address", entry.getKey());
            values.setProperty(prefix + "blocks", Long.toString(totals.blocks));
            values.setProperty(prefix + "money", Long.toString(totals.money));
            values.setProperty(prefix + "shards", Long.toString(totals.shards));
            values.setProperty(prefix + "activeMillis", Long.toString(totals.activeMillis));
        }
        lastSavedAt = clock.getAsLong();
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(temporary)) {
                values.store(out, "EvoAssist mining statistics by server");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException error) {
            reportError.accept(error);
        }
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        Properties values = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            values.load(in);
            currentServer = values.getProperty("lastServer", "");
            for (String key : values.stringPropertyNames()) {
                if (!key.startsWith("server.") || !key.endsWith(".address")) continue;
                String prefix = key.substring(0, key.length() - "address".length());
                try {
                    Totals totals = new Totals();
                    totals.blocks = nonnegative(values, prefix + "blocks");
                    totals.money = nonnegative(values, prefix + "money");
                    totals.shards = nonnegative(values, prefix + "shards");
                    totals.activeMillis = nonnegative(values, prefix + "activeMillis");
                    servers.put(values.getProperty(key), totals);
                } catch (NumberFormatException error) {
                    reportError.accept(error);
                }
            }
        } catch (IOException error) {
            reportError.accept(error);
        }
    }

    private static long nonnegative(Properties values, String key) {
        return Math.max(0, Long.parseLong(values.getProperty(key, "0")));
    }

    private static long add(long total, long amount) {
        return amount > Long.MAX_VALUE - total ? Long.MAX_VALUE : total + amount;
    }

    private static long perHour(long amount, long elapsed) {
        return (long) Math.floor(amount * (3_600_000.0 / elapsed));
    }
}
