package com.konzy.evo_assist.client.features.rewards;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

public final class RewardStatistics {
    public static final class Totals {
        public BigDecimal money = BigDecimal.ZERO;
        public long shards, tokens, infernalTokens, endTokens, clanPoints, clanExperience, clanGold;
    }

    private final Path file;
    private final Consumer<Exception> reportError;
    private final Consumer<RewardMessageParser.Reward> onReward;
    private final Map<String, Totals> servers = new HashMap<>();
    private String currentServer = "";
    private boolean connected;

    public RewardStatistics(Path file, Consumer<Exception> reportError) {
        this(file, reportError, reward -> {});
    }

    public RewardStatistics(Path file, Consumer<Exception> reportError,
                            Consumer<RewardMessageParser.Reward> onReward) {
        this.file = file;
        this.reportError = reportError;
        this.onReward = onReward;
        load();
    }

    public Totals current() { return servers.computeIfAbsent(currentServer, key -> new Totals()); }
    public boolean hasServer() { return !currentServer.isEmpty(); }
    public boolean isConnected() { return connected; }
    public boolean hasBossStatistics() {
        return hasBossRewardStatistics() || hasBossTokenStatistics();
    }

    public boolean hasBossRewardStatistics() {
        if (!hasServer()) return false;
        Totals totals = current();
        return totals.money.signum() != 0 || totals.shards != 0;
    }

    public boolean hasBossTokenStatistics() {
        if (!hasServer()) return false;
        Totals totals = current();
        return totals.tokens != 0 || totals.infernalTokens != 0 || totals.endTokens != 0;
    }

    public boolean hasClanStatistics() {
        if (!hasServer()) return false;
        Totals totals = current();
        return totals.clanPoints != 0 || totals.clanExperience != 0 || totals.clanGold != 0;
    }

    public void connect(String address) {
        currentServer = address.strip().toLowerCase(Locale.ROOT);
        if (currentServer.endsWith(":25565")) currentServer = currentServer.substring(0, currentServer.length() - 6);
        connected = !currentServer.isEmpty();
        current();
        save();
    }

    public void disconnect() {
        save();
        connected = false;
    }

    public void receive(String message) {
        if (!connected) return;
        boolean changed = false;
        for (String line : message.split("\\R")) {
            var reward = RewardMessageParser.parse(line);
            if (reward.isEmpty()) continue;
            try {
                Totals totals = current();
                var value = reward.get();
                switch (value.type()) {
                    case MONEY -> totals.money = totals.money.add(value.amount());
                    case SHARDS -> totals.shards = Math.addExact(totals.shards, value.amount().longValueExact());
                    case TOKENS -> totals.tokens = Math.addExact(totals.tokens, value.amount().longValueExact());
                    case INFERNAL_TOKENS -> totals.infernalTokens = Math.addExact(totals.infernalTokens, value.amount().longValueExact());
                    case END_TOKENS -> totals.endTokens = Math.addExact(totals.endTokens, value.amount().longValueExact());
                    case CLAN_POINTS -> totals.clanPoints = Math.addExact(totals.clanPoints, value.amount().longValueExact());
                    case CLAN_EXPERIENCE -> totals.clanExperience = Math.addExact(totals.clanExperience, value.amount().longValueExact());
                    case CLAN_GOLD -> totals.clanGold = Math.addExact(totals.clanGold, value.amount().longValueExact());
                }
                changed = true;
                onReward.accept(value);
            } catch (ArithmeticException error) {
                reportError.accept(error);
            }
        }
        if (changed) save();
    }

    public void resetBosses() {
        resetBossRewards();
        resetBossTokens();
    }

    public void resetBossRewards() {
        if (!hasServer()) return;
        Totals totals = current();
        totals.money = BigDecimal.ZERO;
        totals.shards = 0;
        save();
    }

    public void resetBossTokens() {
        if (!hasServer()) return;
        Totals totals = current();
        totals.tokens = 0;
        totals.infernalTokens = 0;
        totals.endTokens = 0;
        save();
    }

    public void resetClan() {
        if (!hasServer()) return;
        Totals totals = current();
        totals.clanPoints = 0;
        totals.clanExperience = 0;
        totals.clanGold = 0;
        save();
    }

    public void save() {
        Properties values = new Properties();
        values.setProperty("lastServer", currentServer);
        for (var entry : servers.entrySet()) {
            if (entry.getKey().isEmpty()) continue;
            String prefix = "server." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(entry.getKey().getBytes(StandardCharsets.UTF_8)) + ".";
            Totals totals = entry.getValue();
            values.setProperty(prefix + "address", entry.getKey());
            values.setProperty(prefix + "money", totals.money.toPlainString());
            values.setProperty(prefix + "shards", Long.toString(totals.shards));
            values.setProperty(prefix + "tokens", Long.toString(totals.tokens));
            values.setProperty(prefix + "infernalTokens", Long.toString(totals.infernalTokens));
            values.setProperty(prefix + "endTokens", Long.toString(totals.endTokens));
            values.setProperty(prefix + "clanPoints", Long.toString(totals.clanPoints));
            values.setProperty(prefix + "clanExperience", Long.toString(totals.clanExperience));
            values.setProperty(prefix + "clanGold", Long.toString(totals.clanGold));
        }
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(temporary)) {
                values.store(out, "EvoAssist boss and clan rewards by server");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
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
                    totals.money = new BigDecimal(values.getProperty(prefix + "money", "0")).max(BigDecimal.ZERO);
                    totals.shards = nonnegative(values, prefix + "shards");
                    totals.tokens = nonnegative(values, prefix + "tokens");
                    totals.infernalTokens = nonnegative(values, prefix + "infernalTokens");
                    totals.endTokens = nonnegative(values, prefix + "endTokens");
                    totals.clanPoints = nonnegative(values, prefix + "clanPoints");
                    totals.clanExperience = nonnegative(values, prefix + "clanExperience");
                    totals.clanGold = nonnegative(values, prefix + "clanGold");
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
}
