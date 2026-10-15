package com.konzy.evo_assist.client.features.fishing;

import com.konzy.evo_assist.client.features.fishing.FishingData.Rarity;
import com.konzy.evo_assist.client.features.fishing.FishingData.World;
import com.konzy.evo_assist.client.features.fishing.FishingGoals.Goal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class FishingChecks {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void rejects(Runnable operation, String message) {
        try { operation.run(); throw new AssertionError(message); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static FishingData.ItemExperience item(String name, int count, String... lore) {
        return FishingData.itemExperience(name, count, List.of(lore), true);
    }
    public static void main(String[] args) throws Exception {
        check(FishingData.nextLevelExperience(Rarity.LEGENDARY, 18) == 819200,
                "End level 18 matches the supplied pet screenshot");
        check(FishingData.requiredExperience(Rarity.LEGENDARY, 18, 19, 663552) == 155648,
                "The current pet experience is subtracted from the next-level cost");
        check(FishingData.requiredExperience(Rarity.LEGENDARY, 16, 20, 0) == 1638400,
                "The 16-to-20 goal uses the selected target threshold without summing intermediate levels");
        check(FishingData.requiredExperience(Rarity.LEGENDARY, 16, 20, 100000) == 1538400,
                "Entered experience at the current level is subtracted even when the target is several levels ahead");
        long[][] maximumTargets = {{10240, 25600, 102400}, {327680, 819200, 3276800}, {327680, 819200, 3276800}};
        for (World world : World.values()) {
            for (Rarity rarity : Rarity.values()) {
                check(FishingData.requiredExperience(rarity, world.firstLevel(), world.maxLevel(), 0)
                        == maximumTargets[world.ordinal()][rarity.ordinal()],
                        "Maximum target threshold for " + world + " / " + rarity);
            }
        }
        check(FishingData.requiredExperience(Rarity.COMMON, 20, 21, 0) == 327680,
                "Level 21 can be reached using the repeated Nether scale");
        rejects(() -> FishingData.requiredExperience(Rarity.COMMON, 1, 1, 0), "Same-level goal accepted");
        rejects(() -> FishingData.requiredExperience(Rarity.COMMON, 20, 22, 0), "Level above 21 accepted");
        rejects(() -> FishingData.requiredExperience(Rarity.COMMON, 1, 2, 20), "Invalid pet progress accepted");
        check(item("§bЗмееголов", 3).experience() == 768, "Double and triple catches use actual stack counts");
        check(item("§5Искажённый окунь", 2).experience() == 256, "Names tolerate colors and е/ё");
        check(item("Бочка", 1, "Опыт питомца в ведре: 500/20000") == null,
                "An unrelated item with matching lore is not treated as a bucket");
        check(item("Cod", 1) == null, "Unmapped English item names are not guessed");
        var regular = item("§eВедро для рыбы!", 1, "Владелец: Konnzyy", "§9Опыта питомца в ведре: §b15000/20000");
        check(regular.world() == World.OVERWORLD && regular.experience() == 15000,
                "Normal bucket reads the current experience, not its capacity");
        var nether = item("§dВедро для адской рыбы!", 1, "Опыт питомца в ведре: 398193/1000000");
        check(nether.world() == World.NETHER && nether.experience() == 398193, "Nether bucket screenshot is parsed");
        var end = item("Ведро для эндер рыбы", 1, "Опыта питомца в ведре: 0/1500000");
        check(end.world() == World.END && end.experience() == 0, "Empty End bucket contributes zero");
        for (String bucket : List.of("Ведро для рыбы!", "Ведро для адской рыбы!", "Ведро для эндер рыбы")) {
            check(FishingData.itemExperience(bucket, 1, List.of("Опыт питомца в ведре: 15000/20000"), false) == null,
                    "Bucket experience is excluded when disabled: " + bucket);
        }
        check(FishingData.itemExperience("§bЗмееголов", 3, List.of(), false).experience() == 768,
                "Disabling bucket experience still counts loose fish and stack sizes");
        check(item("Ведро для эндер рыбы", 1, "Опыт питомца в ведре: 1\u00a0500 / 1\u00a0500\u00a0000").experience() == 1500,
                "Grouping spaces in bucket tooltips are supported");
        check(item("Ведро для рыбы", 1, "Опыт питомца в ведре: ?/20000") == null,
                "An unreadable bucket value never credits its capacity");
        check(item("Ведро для рыбы", 1, "Опыт питомца в ведре: 99999999999999999999/20000") == null,
                "Overflowing item data cannot corrupt progress");

        Path dir = Files.createTempDirectory(Files.createDirectories(Path.of(args[0])), "fishing-");
        Path file = dir.resolve("goals.properties");
        var notifications = new AtomicInteger();
        var goals = new FishingGoals(file, goal -> notifications.incrementAndGet(), error -> { throw new AssertionError(error); });
        goals.connect("Test.example");
        var values = new EnumMap<World, Long>(World.class);
        values.put(World.OVERWORLD, 15000L); values.put(World.NETHER, 398193L); values.put(World.END, 256L);
        goals.updateInventory(values);
        var target = Goal.custom(World.NETHER, 400000);
        goals.setGoal(target);
        check(goals.progress() == 398193 && goals.remaining() == 1807,
                "Only fish and buckets from the selected world contribute to the goal");
        goals.updateInventory(values);
        check(goals.progress() == 398193, "Repeated inventory snapshots do not accumulate the same items twice");
        values.put(World.NETHER, 400001L); goals.updateInventory(values);
        check(goals.complete() && goals.remaining() == 0 && notifications.get() == 1,
                "A completed goal clamps remaining experience and notifies once");
        values.put(World.NETHER, 0L); goals.updateInventory(values);
        check(goals.progress() == 0 && !goals.complete(), "Removing or consuming fish decreases live inventory progress");
        values.put(World.NETHER, 400001L); goals.updateInventory(values);
        check(notifications.get() == 1, "Moving fish in and out cannot repeatedly notify completion");
        goals.disconnect(); goals.connect("another.example");
        check(goals.goal() == null && !goals.ready(), "Goals do not leak across servers");
        goals.setGoal(Goal.level(World.END, Rarity.LEGENDARY, 16, 20, 100000));
        // Simulate a goal file from the build that summed all intermediate transitions.
        var saved = new java.util.Properties();
        try (var input = Files.newInputStream(file)) { saved.load(input); }
        for (String key : saved.stringPropertyNames()) {
            if (key.endsWith(".address") && saved.getProperty(key).equals("another.example"))
                saved.setProperty(key.substring(0, key.length() - "address".length()) + "targetExperience", "2972000");
        }
        try (var output = Files.newOutputStream(file)) { saved.store(output, "Legacy summed target"); }
        var restored = new FishingGoals(file, goal -> notifications.incrementAndGet(), error -> { throw new AssertionError(error); });
        restored.connect("test.EXAMPLE"); restored.updateInventory(values);
        check(restored.goal().equals(target) && notifications.get() == 1,
                "The saved goal and one-time notification survive reconnects and restarts");
        restored.connect("another.example");
        check(restored.goal().targetExperience() == 1538400, "Saved level goals are recalculated from original inputs using the corrected threshold");
        restored.reset();
        var reset = new FishingGoals(file, goal -> {}, error -> { throw new AssertionError(error); });
        reset.connect("another.example");
        check(reset.goal() == null, "Reset removes the active goal from disk");
        reset.connect("test.example");
        check(reset.goal().equals(target), "Reset preserves goals belonging to other servers");
        System.out.println("Fishing checks passed: " + checks);
    }
}
