package com.konzy.evo_assist.client.features.mine;

import static com.konzy.evo_assist.client.util.Texts.tr;
import com.konzy.evo_assist.client.Evo_assistClient;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.config.ConfigClan;
import com.konzy.evo_assist.client.config.ConfigVisual;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import com.konzy.evo_assist.client.util.TimeUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.Locale;
import java.util.ArrayDeque;

public final class MiningGoals {
    public static final class Goal {
        public long blocks;
        public long activeMillis;
        public long money;
        public long shards;
        public boolean complete;
        private int target;
        private int pendingPriceBlocks;

        private void reset(int newTarget) {
            blocks = 0;
            activeMillis = 0;
            money = 0;
            shards = 0;
            complete = false;
            target = newTarget;
            pendingPriceBlocks = 0;
        }

        public int target() { return target; }
        public boolean active() { return target > 0; }
    }

    private static MiningGoals instance;
    private final Path file = FabricLoader.getInstance().getConfigDir().resolve("evoassist_mining_goals.properties");
    private final Goal blocks = new Goal();
    private final Goal time = new Goal();
    private long lastSave;
    private String notice;
    private boolean clanNotice;
    private record Notice(String message, boolean clan) {}
    private final ArrayDeque<Notice> pendingNotices = new ArrayDeque<>();
    private long noticeUntil;

    private MiningGoals() {
        load();
        syncTargets();
    }

    public static MiningGoals getInstance() {
        if (instance == null) instance = new MiningGoals();
        return instance;
    }

    public Goal blockGoal() { return blocks; }
    public Goal timeGoal() { return time; }

    public String previewNotice() {
        if (blocks.active()) return blockNotice(blocks.target);
        if (time.active()) return timeNotice(time.target);
        return blockNotice(10_000);
    }

    private static String blockNotice(int target) {
        String amount = String.format(Locale.ROOT, "%,d", target).replace(',', ' ');
        return tr("evoassist.notice.blocks", amount);
    }

    private static String timeNotice(int minutes) {
        long millis = minutes * 60_000L;
        return tr("evoassist.notice.time", TimeUtils.asTextTime(millis));
    }

    public void syncTargets() {
        int blockTarget = Math.max(0, ConfigMining.blockGoalTarget);
        int timeTarget = Math.max(0, ConfigMining.timeGoalMinutes);
        boolean changed = false;
        if (blocks.target != blockTarget) {
            blocks.reset(blockTarget);
            changed = true;
        }
        if (time.target != timeTarget) {
            time.reset(timeTarget);
            changed = true;
        }
        if (changed) save();
    }

    public void resetAll() {
        blocks.reset(Math.max(0, ConfigMining.blockGoalTarget));
        time.reset(Math.max(0, ConfigMining.timeGoalMinutes));
        clearNotices(false);
        AdditionalGoals.getInstance().reset(false);
        save();
    }

    public void blockBroken(boolean pricePending) {
        syncTargets();
        if (blocks.active() && !blocks.complete) {
            blocks.blocks++;
            if (pricePending) blocks.pendingPriceBlocks++;
            if (blocks.blocks >= blocks.target) finish(blocks, blockNotice(blocks.target));
        }
        if (time.active() && !time.complete) {
            time.blocks++;
            if (pricePending) time.pendingPriceBlocks++;
        }
        savePeriodically();
    }

    public void priceForPendingBlocks(long pricePerBlock) {
        if (pricePerBlock <= 0) return;
        AdditionalGoals.getInstance().priceForPendingBlocks(pricePerBlock);
        if (blocks.pendingPriceBlocks > 0) {
            blocks.money += pricePerBlock * blocks.pendingPriceBlocks;
            blocks.pendingPriceBlocks = 0;
        }
        if (time.pendingPriceBlocks > 0) {
            time.money += pricePerBlock * time.pendingPriceBlocks;
            time.pendingPriceBlocks = 0;
        }
        save();
    }

    public void expirePendingPrices() {
        AdditionalGoals.getInstance().expirePendingPrices();
        blocks.pendingPriceBlocks = 0;
        time.pendingPriceBlocks = 0;
    }

    public void moneyEarned(long amount) {
        if (amount <= 0) return;
        AdditionalGoals.getInstance().moneyEarned(amount);
        syncTargets();
        if (blocks.active() && !blocks.complete) blocks.money += amount;
        if (time.active() && !time.complete) time.money += amount;
        savePeriodically();
    }

    public void shardsEarned(long amount) {
        if (amount <= 0) return;
        AdditionalGoals.getInstance().shardsEarned(amount);
        syncTargets();
        if (blocks.active() && !blocks.complete) blocks.shards += amount;
        if (time.active() && !time.complete) time.shards += amount;
        savePeriodically();
    }

    public void activeSecond() {
        AdditionalGoals.getInstance().activeSecond();
        syncTargets();
        if (blocks.active() && !blocks.complete) blocks.activeMillis += 1_000;
        if (time.active() && !time.complete) {
            time.activeMillis += 1_000;
            if (time.activeMillis >= time.target * 60_000L) finish(time, timeNotice(time.target));
        }
        savePeriodically();
    }

    private void finish(Goal goal, String message) {
        goal.complete = true;
        if (ConfigMining.goalNotifications) {
            showNotice(message, false);
        }
        save();
    }

    public String currentNotice() {
        if (System.currentTimeMillis() >= noticeUntil) notice = null;
        if (notice != null && !(clanNotice ? ConfigClan.goalNotifications : ConfigMining.goalNotifications)) notice = null;
        while (notice == null && !pendingNotices.isEmpty()) {
            Notice next = pendingNotices.removeFirst();
            if (next.clan ? ConfigClan.goalNotifications : ConfigMining.goalNotifications) {
                notice = next.message;
                clanNotice = next.clan;
                noticeUntil = System.currentTimeMillis() + ConfigVisual.goalNoticeDurationMillis();
            }
        }
        return (clanNotice ? ConfigClan.goalNotifications : ConfigMining.goalNotifications)
                && notice != null && System.currentTimeMillis() < noticeUntil
                ? notice : null;
    }

    public void showNotice(String message, boolean clan) {
        if (notice != null && System.currentTimeMillis() < noticeUntil) {
            pendingNotices.addLast(new Notice(message, clan));
            return;
        }
        if (!pendingNotices.isEmpty()) {
            pendingNotices.addLast(new Notice(message, clan));
            currentNotice();
            return;
        }
        notice = message;
        clanNotice = clan;
        noticeUntil = System.currentTimeMillis() + ConfigVisual.goalNoticeDurationMillis();
    }

    public void clearNotices(boolean clan) {
        pendingNotices.removeIf(item -> item.clan == clan);
        if (clanNotice == clan) { notice = null; noticeUntil = 0; }
    }

    private void savePeriodically() {
        if (System.currentTimeMillis() - lastSave >= 5_000) save();
    }

    public void save() {
        Properties values = new Properties();
        storeGoal(values, "blocks", blocks);
        storeGoal(values, "time", time);
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(temporary)) {
                values.store(out, "EvoAssist mining goals");
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            lastSave = System.currentTimeMillis();
        } catch (IOException error) {
            Evo_assistClient.logger.warn("Could not save mining goals", error);
        }
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        Properties values = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            values.load(in);
            loadGoal(values, "blocks", blocks);
            loadGoal(values, "time", time);
        } catch (IOException | NumberFormatException error) {
            Evo_assistClient.logger.warn("Could not load mining goals", error);
            blocks.reset(0);
            time.reset(0);
        }
    }

    private static void storeGoal(Properties values, String prefix, Goal goal) {
        values.setProperty(prefix + ".target", Integer.toString(goal.target));
        values.setProperty(prefix + ".blocks", Long.toString(goal.blocks));
        values.setProperty(prefix + ".activeMillis", Long.toString(goal.activeMillis));
        values.setProperty(prefix + ".money", Long.toString(goal.money));
        values.setProperty(prefix + ".shards", Long.toString(goal.shards));
        values.setProperty(prefix + ".complete", Boolean.toString(goal.complete));
    }

    private static void loadGoal(Properties values, String prefix, Goal goal) {
        goal.target = Math.max(0, Integer.parseInt(values.getProperty(prefix + ".target", "0")));
        goal.blocks = Math.max(0, Long.parseLong(values.getProperty(prefix + ".blocks", "0")));
        goal.activeMillis = Math.max(0, Long.parseLong(values.getProperty(prefix + ".activeMillis", "0")));
        goal.money = Math.max(0, Long.parseLong(values.getProperty(prefix + ".money", "0")));
        goal.shards = Math.max(0, Long.parseLong(values.getProperty(prefix + ".shards", "0")));
        goal.complete = Boolean.parseBoolean(values.getProperty(prefix + ".complete", "false"));
    }
}
