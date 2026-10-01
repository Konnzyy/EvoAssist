package com.konzy.evo_assist.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category("Визуал")
public class ConfigVisual {
    @ConfigOption.Range(min = 10, max = 100)
    @ConfigOption.Slider
    @ConfigEntry(
            id = "menuOpacity",
            translation = "evoassist.ui.menuOpacity.title"
    )
    public static int menuOpacity = 90;

    @ConfigOption.Range(min = 1, max = 60)
    @ConfigOption.Slider
    @ConfigEntry(id = "goalNoticeDurationSeconds", translation = "evoassist.ui.goalNoticeDuration.title")
    public static int goalNoticeDurationSeconds = 5;

    public static long goalNoticeDurationMillis() {
        return Math.max(1, Math.min(60, goalNoticeDurationSeconds)) * 1_000L;
    }

}
