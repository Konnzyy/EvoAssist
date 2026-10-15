package com.konzy.evo_assist.client.config;

import com.konzy.evo_assist.client.features.fishing.FishingData;
import com.konzy.evo_assist.client.features.fishing.FishingData.Rarity;
import com.konzy.evo_assist.client.features.fishing.FishingData.World;
import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;

/** Draft goal inputs and general fishing settings. */
@Category("Рыбалка")
public final class ConfigFishing {
    @ConfigEntry(id = "world")
    public static World world = World.OVERWORLD;
    @ConfigEntry(id = "rarity")
    public static Rarity rarity = Rarity.COMMON;
    @ConfigEntry(id = "currentLevel")
    public static int currentLevel = 1;
    @ConfigEntry(id = "targetLevel")
    public static int targetLevel = 2;
    @ConfigEntry(id = "currentExperience")
    public static int currentExperience = 0;
    @ConfigEntry(id = "customExperience")
    public static String customExperience = "0";
    @ConfigEntry(id = "customWorld")
    public static World customWorld = World.OVERWORLD;
    @ConfigEntry(id = "goalNotifications")
    public static boolean goalNotifications = true;
    @ConfigEntry(id = "includeBuckets")
    public static boolean includeBuckets = true;

    public static void normalize() {
        normalizeLevels();
        if (customWorld == null) customWorld = World.OVERWORLD;
        if (rarity == null) rarity = Rarity.COMMON;
        currentExperience = Math.clamp(currentExperience, 0, (int) FishingData.nextLevelExperience(rarity, currentLevel) - 1);
        try {
            if (Long.parseLong(customExperience) < 0) customExperience = "0";
        } catch (NumberFormatException ignored) { customExperience = "0"; }
    }

    public static void normalizeLevels() {
        if (world == null) world = World.OVERWORLD;
        currentLevel = Math.clamp(currentLevel, world.firstLevel(), world.maxLevel() - 1);
        targetLevel = Math.clamp(targetLevel, currentLevel + 1, world.maxLevel());
    }
}
