/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.config;

import static com.konzy.evo_assist.client.util.Texts.tr;
import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category("Чат")
public class ConfigChat {
    // Retained for old config compatibility; EvoPlus owns visibility.
    @ConfigOption.Hidden
    @ConfigEntry(
            id = "chatTabsToggle",
            translation = "evoassist.ui.chatTabs.title"
    )
    public static boolean chatTabsToggle = true;
    @ConfigOption.Hidden
    @ConfigEntry(id = "chatWidgetMigratedToEvoPlus")
    public static boolean chatWidgetMigratedToEvoPlus = false;
    @ConfigEntry(id = "chatTabsOrientation", translation = "evoassist.ui.chatOrientation.title")
    public static Orientation chatTabsOrientation = Orientation.HORIZONTAL;

    public enum Orientation {
        HORIZONTAL, VERTICAL;
        @Override
        public String toString() { return this == HORIZONTAL ? tr("evoassist.ui.horizontal") : tr("evoassist.ui.vertical"); }
    }
}
