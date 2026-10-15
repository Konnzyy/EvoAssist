package com.konzy.evo_assist.client.features.mine.blockPH;

import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals.Type;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ConfirmedMiningChecks {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory(Files.createDirectories(Path.of(args[0])), "confirmed-");
        var now = new AtomicLong(10_000);
        var targets = new EnumMap<Type, String>(Type.class);
        for (var type : Type.values()) targets.put(type, type.clan ? "10" : "1Q");
        var notices = new ArrayList<String>();
        var additional = new AdditionalGoals(dir.resolve("additional.properties"), targets::get,
                (type, text) -> notices.add(text), error -> { throw new AssertionError(error); });
        var blockTarget = new AtomicInteger(3);
        var goals = new MiningGoals(dir.resolve("mining-goals.properties"), blockTarget::get, () -> 1,
                additional, notices::add, error -> { throw new AssertionError(error); });
        var stats = new MiningStatistics(dir.resolve("mining.properties"),
                error -> { throw new AssertionError(error); }, now::get);
        stats.connect("test.example");
        var rewards = new ConfirmedBlockRewards(money -> {
            stats.record(0, money, 0);
            goals.moneyEarned(money);
        });
        var breaks = new ServerConfirmedBlockBreaks(() -> {
            stats.record(1, 0, 0);
            goals.blockBroken();
        });

        confirm(breaks, new net.minecraft.core.BlockPos(1, 64, 1));
        rewards.actionBar("§a+1.25B§f\uE135");
        check(stats.current().blocks == 1 && stats.current().money == 1_250_000_000L,
                "A server-confirmed block and its received money update are counted separately");
        confirm(breaks, new net.minecraft.core.BlockPos(2, 64, 1));
        rewards.actionBar("+2,5B$");
        now.addAndGet(5_000);
        rewards.actionBar("+500M$");
        confirm(breaks, new net.minecraft.core.BlockPos(3, 64, 1));
        check(stats.current().blocks == 3 && goals.blockGoal().blocks == 3 && goals.blockGoal().complete,
                "Only matching server block-state confirmations advance block goals");
        check(goals.blockGoal().money == 4_250_000_000L && goals.timeGoal().money == stats.current().money
                && additional.goal(Type.MONEY).money.compareTo(BigDecimal.valueOf(stats.current().money)) == 0,
                "The final reward and changing prices agree across active goals and mining statistics");
        int noticeCount = notices.size();
        rewards.actionBar("+10M$");
        check(goals.blockGoal().blocks == 3 && goals.blockGoal().money == 4_250_000_000L
                && notices.size() == noticeCount, "Completed goal stays frozen without duplicate notifications");

        long blocks = stats.current().blocks;
        for (String noise : new String[]{"+0$", "Баланс: 100B", "+10%", "+1.2.3B$", "+1K опыта",
                "+10\uE365", "+999999999999999999999Q$", "", "Вы сломали блок"}) rewards.actionBar(noise);
        check(stats.current().blocks == blocks, "Money, XP, shards, balances and invalid overlays do not change block totals");

        long money = stats.current().money;
        // The action bar is rate-limited and may aggregate money across many blocks.
        for (int i = 0; i < 10_000; i++) rewards.actionBar("+1M$");
        check(stats.current().blocks == blocks && stats.current().money == money + 10_000_000_000L,
                "Repeated reward updates add money without treating each update as one broken block");
        check(goals.timeGoal().blocks == stats.current().blocks && additional.goal(Type.SHARDS).blocks == stats.current().blocks,
                "All running goals share the server-confirmed block count");
        check(ConfirmedBlockRewards.rewardAmount("+1.5M") == 1_500_000
                && ConfirmedBlockRewards.rewardAmount("+15%  +2,5M\uE135") == 2_500_000,
                "Legacy bare money and compound action bars are supported");

        for (int i = 0; i < 60; i++) goals.activeSecond();
        check(goals.timeGoal().complete && goals.timeGoal().activeMillis == 60_000, "Time goal completion is preserved");
        long timeBlocks = goals.timeGoal().blocks;
        rewards.actionBar("+1M$");
        check(goals.timeGoal().blocks == timeBlocks, "Completed time goal stays frozen");
        goals.save(); additional.save(); stats.disconnect();
        var restored = new MiningGoals(dir.resolve("mining-goals.properties"), blockTarget::get, () -> 1,
                additional, notices::add, error -> { throw new AssertionError(error); });
        check(restored.blockGoal().complete && restored.blockGoal().money == 4_250_000_000L
                && restored.timeGoal().blocks == timeBlocks, "Confirmed progress persists across restart");
        goals.resetAll();
        check(!goals.hasProgress() && !additional.hasProgress(false) && goals.blockGoal().target() == 3,
                "Reset all progress preserves targets and clears every mining goal");
        System.out.println("Confirmed mining checks passed: " + checks);
    }

    private static void confirm(ServerConfirmedBlockBreaks breaks, net.minecraft.core.BlockPos pos) {
        breaks.localBreak(pos);
        if (!breaks.serverUpdate(pos, true)) throw new AssertionError("Expected same-position server air update to confirm break");
    }
}
