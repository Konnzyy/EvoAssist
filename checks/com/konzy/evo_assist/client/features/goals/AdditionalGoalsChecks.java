package com.konzy.evo_assist.client.features.goals;

import com.konzy.evo_assist.client.features.goals.AdditionalGoals.Type;
import com.konzy.evo_assist.client.features.rewards.RewardMessageParser;
import com.konzy.evo_assist.client.util.GoalAmount;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;

public final class AdditionalGoalsChecks {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void equal(BigDecimal actual, String expected, String message) {
        check(actual.compareTo(new BigDecimal(expected)) == 0, message + ": " + actual);
    }
    private static void rejected(String value, boolean whole) {
        try { GoalAmount.parse(value, whole); throw new AssertionError("Accepted invalid amount: " + value); }
        catch (IllegalArgumentException expected) {}
    }

    public static void main(String[] args) throws Exception {
        equal(GoalAmount.parse("1.1K", true), "1100", "K parsing");
        equal(GoalAmount.parse("2.15m", true), "2150000", "Case insensitive M parsing");
        equal(GoalAmount.parse("1.1B", false), "1100000000", "B parsing");
        equal(GoalAmount.parse("4.43T", false), "4430000000000", "T parsing");
        equal(GoalAmount.parse("5.23Q", false), "5230000000000000", "Exact Q parsing");
        equal(GoalAmount.parse("1.1", false), "1.1", "Fractional money");
        equal(GoalAmount.parse("9223372036854775807", true), "9223372036854775807", "Maximum value");
        for (String value : new String[] { "-1", "1..2B", "B", "1E9", "1B2", "1QQ", "9223372036854775808" })
            rejected(value, false);
        rejected("1.1", true);
        for (Type type : Type.values()) {
            String maximum = type.clan ? "1M" : "999Q";
            equal(AdditionalGoals.parseTarget(type, maximum),
                    type.clan ? "1000000" : "999000000000000000", "Goal maximum: " + type);
            String excessive = type.clan ? "1000001" : "999.000000000000001Q";
            try {
                AdditionalGoals.parseTarget(type, excessive);
                throw new AssertionError("Accepted goal above limit: " + type);
            } catch (IllegalArgumentException expected) {}
        }

        Path folder = Path.of(args[0]);
        Files.createDirectories(folder);
        Path file = Files.createTempFile(folder, "goals-", ".properties");
        var targets = new EnumMap<Type, String>(Type.class);
        targets.put(Type.MONEY, "1.1B");
        targets.put(Type.SHARDS, "3");
        targets.put(Type.CLAN_POINTS, "5");
        targets.put(Type.CLAN_GOLD, "3");
        targets.put(Type.CLAN_EXPERIENCE, "2");
        var notices = new ArrayList<String>();
        var errors = new ArrayList<Exception>();
        var goals = new AdditionalGoals(file, targets::get, (type, message) -> notices.add(message), errors::add);
        check(!goals.hasProgress(false) && !goals.hasProgress(true), "Configured targets alone do not enable reset");
        check(!goals.goal(Type.MONEY).wouldResetProgress(new BigDecimal("2000000000")), "No warning for a goal without progress");
        goals.activeSecond();
        check(goals.goal(Type.MONEY).wouldResetProgress(new BigDecimal("2000000000")), "Changing unfinished time-only goal requires confirmation");
        check(!goals.goal(Type.MONEY).wouldResetProgress(new BigDecimal("1100000000.00")), "Numerically equal target preserves progress without warning");
        equal(goals.goal(Type.MONEY).target, "1100000000", "Checking confirmation does not change the target");
        check(goals.goal(Type.MONEY).activeMillis == 1000, "Checking confirmation preserves existing progress");
        check(goals.hasProgress(false) && !goals.hasProgress(true), "Elapsed mining time alone enables mining reset");
        goals.reset(false);
        check(!goals.hasProgress(false), "Reset clears time-only statistics");
        goals.blockBroken(false);
        check(goals.hasProgress(false), "Unpriced mined blocks enable reset");
        goals.moneyEarned(550_000_000);
        goals.shardsEarned(1);
        goals.activeSecond();
        goals.blockBroken(false);
        goals.moneyEarned(550_000_000);
        check(goals.goal(Type.MONEY).complete, "Money completion");
        check(!goals.goal(Type.MONEY).wouldResetProgress(new BigDecimal("2000000000")), "Completed goal can be replaced without unfinished-goal warning");
        check(goals.goal(Type.MONEY).blocks == 2, "Final block included in money goal");
        goals.shardsEarned(5);
        goals.moneyEarned(100);
        goals.blockBroken(false);
        goals.activeSecond();
        equal(goals.goal(Type.MONEY).progress, "1100000000", "Money progress frozen");
        equal(goals.goal(Type.MONEY).money, "1100000000", "Money statistics frozen");
        check(goals.goal(Type.MONEY).blocks == 2 && goals.goal(Type.MONEY).shards == 1
                && goals.goal(Type.MONEY).activeMillis == 1000, "Completed money metadata frozen");
        equal(goals.goal(Type.SHARDS).progress, "3", "Shard progress capped at target");
        check(goals.goal(Type.SHARDS).complete, "Shard completion");

        goals.connect("Example.org:25565");
        goals.reward(new RewardMessageParser.Reward(RewardMessageParser.Type.CLAN_POINTS, BigDecimal.valueOf(3)));
        goals.connect("other.org");
        check(!goals.hasProgress(true) && goals.hasProgress(false), "Clan reset availability follows server independently");
        equal(goals.goal(Type.CLAN_POINTS).progress, "0", "Server isolation");
        goals.connect("EXAMPLE.ORG");
        equal(goals.goal(Type.CLAN_POINTS).progress, "3", "Normalized server address");
        goals.reward(new RewardMessageParser.Reward(RewardMessageParser.Type.CLAN_POINTS, BigDecimal.valueOf(7)));
        goals.reward(new RewardMessageParser.Reward(RewardMessageParser.Type.CLAN_POINTS, BigDecimal.valueOf(10)));
        equal(goals.goal(Type.CLAN_POINTS).progress, "5", "Clan progress stops at completion");
        goals.reward(new RewardMessageParser.Reward(RewardMessageParser.Type.CLAN_GOLD, BigDecimal.valueOf(1)));
        goals.disconnect();
        var restored = new AdditionalGoals(file, targets::get, (type, message) -> notices.add(message), errors::add);
        restored.connect("example.org");
        check(restored.goal(Type.MONEY).complete, "Completed state persisted");
        check(restored.goal(Type.MONEY).activeMillis == 1000, "Time persisted");
        equal(restored.goal(Type.CLAN_GOLD).progress, "1", "Clan progress persisted");
        check(restored.goal(Type.CLAN_POINTS).complete, "Clan completion persisted");
        restored.reset(true);
        check(!restored.hasProgress(true) && restored.hasProgress(false), "Clan reset clears only clan progress");
        equal(restored.goal(Type.CLAN_POINTS).progress, "0", "Clan reset");
        equal(restored.goal(Type.CLAN_POINTS).target, "5", "Clan reset preserves target");
        check(restored.goal(Type.MONEY).complete, "Clan reset independent of mining");
        targets.put(Type.MONEY, "1Q");
        restored.syncTargets();
        equal(restored.goal(Type.MONEY).progress, "0", "New target resets progress");
        check(restored.goal(Type.SHARDS).complete, "Changing one target preserves another");
        restored.blockBroken(true);
        restored.blockBroken(true);
        restored.recordBlockReward(400_000_000_000_000L);
        equal(restored.goal(Type.MONEY).progress, "400000000000000", "First block uses its own price");
        restored.recordBlockReward(600_000_000_000_000L);
        equal(restored.goal(Type.MONEY).progress, "1000000000000000", "Distinct block prices complete the goal");
        equal(restored.goal(Type.MONEY).money, "1000000000000000", "Block prices are not multiplied by pending count");
        check(restored.goal(Type.MONEY).blocks == 2, "Both priced blocks recorded");
        int noticeCount = notices.size();
        restored.recordBlockReward(100L);
        check(notices.size() == noticeCount, "No repeat completion notification");
        restored.reset(false);
        check(!restored.hasProgress(false), "Mining reset clears progress and secondary statistics");
        check(!restored.goal(Type.MONEY).wouldResetProgress(BigDecimal.ZERO), "Empty goal can be disabled without progress warning");
        equal(restored.goal(Type.MONEY).target, "1000000000000000", "Reset preserves Q target");
        restored.blockBroken(true);
        restored.blockBroken(true);
        restored.recordPendingBlockRewards(500_000_000_000_000L);
        equal(restored.goal(Type.MONEY).progress, "1000000000000000", "One sampled price can cover pending blocks");
        equal(restored.goal(Type.MONEY).money, "1000000000000000", "Pending blocks receive a price once each");
        restored.reset(false);
        targets.put(Type.MONEY, "0");
        restored.syncTargets();
        restored.moneyEarned(100);
        check(!restored.goal(Type.MONEY).active(), "Zero disables goal");
        equal(restored.goal(Type.MONEY).progress, "0", "Disabled goal ignores rewards");
        check(errors.isEmpty(), "Persistence errors: " + errors);
        // Apply the new limit to an older saved goal without losing earned progress.
        restored.save();
        var saved = new java.util.Properties();
        try (var in = Files.newInputStream(file)) { saved.load(in); }
        String serverPrefix = saved.stringPropertyNames().stream()
                .filter(key -> key.endsWith(".address") && saved.getProperty(key).equals("example.org"))
                .findFirst().orElseThrow().replace("address", "");
        saved.setProperty(serverPrefix + "CLAN_POINTS.target", "5000000");
        saved.setProperty(serverPrefix + "CLAN_POINTS.progress", "865000");
        saved.setProperty(serverPrefix + "CLAN_POINTS.complete", "false");
        try (var out = Files.newOutputStream(file)) { saved.store(out, "Legacy target"); }
        targets.put(Type.CLAN_POINTS, "5M");
        var migrated = new AdditionalGoals(file, targets::get, (type, message) -> notices.add(message), errors::add);
        migrated.connect("example.org");
        equal(migrated.goal(Type.CLAN_POINTS).target, "1000000", "Saved target limited to 1M");
        equal(migrated.goal(Type.CLAN_POINTS).progress, "865000", "Earned progress preserved when limiting target");
        Files.delete(file);
        System.out.println("Additional goal checks passed: exact amounts, freeze, pending prices, resets, persistence and server isolation.");
    }
}
