package com.konzy.evo_assist.client.features.goals;

import static com.konzy.evo_assist.client.util.Texts.tr;
import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.config.ConfigClan;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import com.konzy.evo_assist.client.features.rewards.RewardMessageParser;
import com.konzy.evo_assist.client.util.GoalAmount;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.BiConsumer;

public final class AdditionalGoals {
    public enum Type {
        MONEY("evoassist.goal.name.money", false), SHARDS("evoassist.goal.name.shards", false),
        CLAN_POINTS("evoassist.goal.name.clanPoints", true), CLAN_GOLD("evoassist.goal.name.clanGold", true), CLAN_EXPERIENCE("evoassist.goal.name.clanExperience", true);
        public final String title;
        public final boolean clan;
        Type(String title, boolean clan) { this.title = title; this.clan = clan; }
        public boolean whole() { return this != MONEY; }
        public BigDecimal maximum() {
            return clan ? BigDecimal.valueOf(1_000_000) : new BigDecimal("999000000000000000");
        }
    }

    public static final class Goal {
        public BigDecimal target = BigDecimal.ZERO;
        public BigDecimal progress = BigDecimal.ZERO;
        public BigDecimal money = BigDecimal.ZERO;
        public long blocks, shards, activeMillis;
        public boolean complete;
        private long pendingBlocks;
        public boolean active() { return target.signum() > 0; }
        public boolean running() { return active() && !complete; }
        public boolean hasProgress() {
            return progress.signum() != 0 || money.signum() != 0 || blocks != 0 || shards != 0
                    || activeMillis != 0 || complete || pendingBlocks != 0;
        }
        public boolean wouldResetProgress(BigDecimal newTarget) {
            return active() && !complete && hasProgress() && target.compareTo(newTarget) != 0;
        }
        private void reset(BigDecimal target) {
            this.target = target;
            progress = BigDecimal.ZERO;
            money = BigDecimal.ZERO;
            blocks = shards = activeMillis = pendingBlocks = 0;
            complete = false;
        }
    }

    private static AdditionalGoals instance;
    private final Path file;
    private final Function<Type, String> targets;
    private final BiConsumer<Type, String> notify;
    private final Consumer<Exception> reportError;
    private final EnumMap<Type, Goal> mining = new EnumMap<>(Type.class);
    private final Map<String, EnumMap<Type, Goal>> clans = new HashMap<>();
    private String currentServer = "";
    private boolean connected;
    private long lastSave;

    public AdditionalGoals(Path file, Function<Type, String> targets, BiConsumer<Type, String> notify,
                           Consumer<Exception> reportError) {
        this.file = file;
        this.targets = targets;
        this.notify = notify;
        this.reportError = reportError;
        mining.put(Type.MONEY, new Goal());
        mining.put(Type.SHARDS, new Goal());
        load();
        syncTargets();
    }

    public static AdditionalGoals getInstance() {
        if (instance == null) instance = new AdditionalGoals(
                FabricLoader.getInstance().getConfigDir().resolve("evoassist_additional_goals.properties"),
                AdditionalGoals::configuredTarget, (type, message) -> {
                    if (type.clan ? ConfigClan.goalNotifications : ConfigMining.goalNotifications)
                        MiningGoals.getInstance().showNotice(message, type.clan);
                },
                error -> EvoAssistClient.logger.warn("Could not update goals", error));
        return instance;
    }

    public static String configuredTarget(Type type) {
        return switch (type) {
            case MONEY -> ConfigMining.moneyGoalTarget;
            case SHARDS -> ConfigMining.shardGoalTarget;
            case CLAN_POINTS -> ConfigClan.pointsGoalTarget;
            case CLAN_GOLD -> ConfigClan.goldGoalTarget;
            case CLAN_EXPERIENCE -> ConfigClan.experienceGoalTarget;
        };
    }

    public static BigDecimal parseTarget(Type type, String input) {
        BigDecimal value = GoalAmount.parse(input, type.whole());
        if (value.compareTo(type.maximum()) > 0)
            throw new IllegalArgumentException(tr("evoassist.error.maximumGoal", type.clan ? "1M" : "999Q"));
        return value;
    }

    public static void setConfiguredTarget(Type type, String value) {
        switch (type) {
            case MONEY -> ConfigMining.moneyGoalTarget = value;
            case SHARDS -> ConfigMining.shardGoalTarget = value;
            case CLAN_POINTS -> ConfigClan.pointsGoalTarget = value;
            case CLAN_GOLD -> ConfigClan.goldGoalTarget = value;
            case CLAN_EXPERIENCE -> ConfigClan.experienceGoalTarget = value;
        }
    }

    public static boolean normalizeConfiguredTargets() {
        boolean changed = false;
        for (Type type : Type.values()) {
            try {
                BigDecimal previous = GoalAmount.parse(configuredTarget(type), type.whole());
                if (previous.compareTo(type.maximum()) > 0) {
                    setConfiguredTarget(type, type.maximum().toPlainString());
                    changed = true;
                }
            } catch (IllegalArgumentException ignored) { }
        }
        return changed;
    }

    private EnumMap<Type, Goal> clanGoals() {
        return clans.computeIfAbsent(currentServer, _ -> {
            var result = new EnumMap<Type, Goal>(Type.class);
            for (Type type : Type.values()) if (type.clan) result.put(type, new Goal());
            return result;
        });
    }

    public Goal goal(Type type) { return type.clan ? clanGoals().get(type) : mining.get(type); }
    public boolean connected() { return connected; }
    public boolean hasProgress(boolean clan) {
        return (clan ? clanGoals() : mining).values().stream().anyMatch(Goal::hasProgress);
    }
    public static String format(Type type, BigDecimal amount) {
        return type.clan ? RewardMessageParser.whole(amount.longValueExact()) : GoalAmount.format(amount);
    }

    public void connect(String address) {
        currentServer = address.strip().toLowerCase(Locale.ROOT);
        if (currentServer.endsWith(":25565")) currentServer = currentServer.substring(0, currentServer.length() - 6);
        connected = !currentServer.isEmpty();
        syncTargets();
        save();
    }

    public void disconnect() { expirePendingPrices(); save(); connected = false; }

    private BigDecimal target(Type type) {
        try { return GoalAmount.parse(targets.apply(type), type.whole()).min(type.maximum()); }
        catch (IllegalArgumentException error) { return BigDecimal.ZERO; }
    }

    public void syncTargets() {
        boolean changed = false;
        for (Type type : Type.values()) {
            Goal goal = goal(type);
            BigDecimal target = target(type);
            if (goal.target.compareTo(target) != 0) {
                if (goal.target.compareTo(type.maximum()) > 0 && target.compareTo(type.maximum()) == 0) {
                    goal.target = target;
                    goal.progress = goal.progress.min(target);
                    goal.complete = goal.progress.compareTo(target) >= 0;
                } else goal.reset(target);
                changed = true;
            }
        }
        if (changed) save();
    }

    public void reset(boolean clan) {
        for (Type type : Type.values()) if (type.clan == clan) goal(type).reset(target(type));
        save();
    }

    private void advance(Type type, BigDecimal amount) {
        Goal goal = goal(type);
        if (!goal.running() || amount.signum() <= 0) return;
        goal.progress = goal.progress.add(amount).min(goal.target);
        if (goal.progress.compareTo(goal.target) >= 0) {
            goal.complete = true;
            notify.accept(type, tr("evoassist.notice.reward", tr(type.title), format(type, goal.target)));
            save();
        }
    }

    public void blockBroken(boolean pendingPrice) {
        syncTargets();
        for (Goal goal : mining.values()) if (goal.running()) {
            goal.blocks++;
            if (pendingPrice) goal.pendingBlocks++;
        }
        savePeriodically();
    }

    public void moneyEarned(long amount) {
        if (amount <= 0) return;
        syncTargets();
        BigDecimal value = BigDecimal.valueOf(amount);
        for (Goal goal : mining.values()) if (goal.running()) goal.money = goal.money.add(value);
        advance(Type.MONEY, value);
        savePeriodically();
    }

    public void recordBlockReward(long price) {
        if (price <= 0) return;
        syncTargets();
        BigDecimal amount = BigDecimal.valueOf(price);
        for (Goal goal : mining.values()) {
            boolean pending = goal.pendingBlocks > 0;
            if (pending) goal.pendingBlocks--;
            if (pending || goal.running()) {
                goal.money = goal.money.add(amount);
            }
        }
        advance(Type.MONEY, amount);
        savePeriodically();
    }

    public void recordPendingBlockRewards(long price) {
        if (price <= 0) return;
        syncTargets();
        for (var entry : mining.entrySet()) {
            Goal goal = entry.getValue();
            long count = goal.pendingBlocks;
            goal.pendingBlocks = 0;
            if (count <= 0) continue;
            BigDecimal amount = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(count));
            goal.money = goal.money.add(amount);
            if (entry.getKey() == Type.MONEY) advance(Type.MONEY, amount);
        }
        savePeriodically();
    }

    public void expirePendingPrices() { mining.values().forEach(goal -> goal.pendingBlocks = 0); }

    public void shardsEarned(long amount) {
        if (amount <= 0) return;
        syncTargets();
        for (Goal goal : mining.values()) if (goal.running()) goal.shards += amount;
        advance(Type.SHARDS, BigDecimal.valueOf(amount));
        savePeriodically();
    }

    public void activeSecond() {
        syncTargets();
        for (Goal goal : mining.values()) if (goal.running()) goal.activeMillis += 1000;
        savePeriodically();
    }

    public void reward(RewardMessageParser.Reward reward) {
        if (!connected) return;
        Type type = switch (reward.type()) {
            case CLAN_POINTS -> Type.CLAN_POINTS;
            case CLAN_GOLD -> Type.CLAN_GOLD;
            case CLAN_EXPERIENCE -> Type.CLAN_EXPERIENCE;
            default -> null;
        };
        if (type == null) return;
        syncTargets();
        advance(type, reward.amount());
        save();
    }

    private void savePeriodically() { if (System.currentTimeMillis() - lastSave >= 5000) save(); }

    public void save() {
        Properties values = new Properties();
        values.setProperty("lastServer", currentServer);
        mining.forEach((type, goal) -> store(values, "mining." + type.name(), goal));
        clans.forEach((server, goals) -> {
            if (server.isEmpty()) return;
            String prefix = "server." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(server.getBytes(StandardCharsets.UTF_8)) + ".";
            values.setProperty(prefix + "address", server);
            goals.forEach((type, goal) -> store(values, prefix + type.name(), goal));
        });
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(temporary)) { values.store(out, "EvoAssist goals"); }
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            lastSave = System.currentTimeMillis();
        } catch (IOException error) { reportError.accept(error); }
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        Properties values = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            values.load(in);
            currentServer = values.getProperty("lastServer", "");
            mining.forEach((type, goal) -> restore(values, "mining." + type.name(), goal));
            for (String key : values.stringPropertyNames()) {
                if (!key.startsWith("server.") || !key.endsWith(".address")) continue;
                String server = values.getProperty(key);
                String prefix = key.substring(0, key.length() - "address".length());
                var goals = new EnumMap<Type, Goal>(Type.class);
                for (Type type : Type.values()) if (type.clan) {
                    Goal goal = new Goal();
                    restore(values, prefix + type.name(), goal);
                    goals.put(type, goal);
                }
                clans.put(server, goals);
            }
        } catch (IOException | NumberFormatException error) { reportError.accept(error); }
    }

    private static void store(Properties values, String prefix, Goal goal) {
        values.setProperty(prefix + ".target", goal.target.toPlainString());
        values.setProperty(prefix + ".progress", goal.progress.toPlainString());
        values.setProperty(prefix + ".money", goal.money.toPlainString());
        values.setProperty(prefix + ".blocks", Long.toString(goal.blocks));
        values.setProperty(prefix + ".shards", Long.toString(goal.shards));
        values.setProperty(prefix + ".activeMillis", Long.toString(goal.activeMillis));
        values.setProperty(prefix + ".complete", Boolean.toString(goal.complete));
    }

    private static void restore(Properties values, String prefix, Goal goal) {
        goal.target = new BigDecimal(values.getProperty(prefix + ".target", "0")).max(BigDecimal.ZERO);
        goal.progress = new BigDecimal(values.getProperty(prefix + ".progress", "0")).max(BigDecimal.ZERO).min(goal.target);
        goal.money = new BigDecimal(values.getProperty(prefix + ".money", "0")).max(BigDecimal.ZERO);
        goal.blocks = Math.max(0, Long.parseLong(values.getProperty(prefix + ".blocks", "0")));
        goal.shards = Math.max(0, Long.parseLong(values.getProperty(prefix + ".shards", "0")));
        goal.activeMillis = Math.max(0, Long.parseLong(values.getProperty(prefix + ".activeMillis", "0")));
        goal.complete = goal.active() && (Boolean.parseBoolean(values.getProperty(prefix + ".complete", "false"))
                || goal.progress.compareTo(goal.target) >= 0);
    }
}
