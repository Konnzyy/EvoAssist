package com.konzy.evo_assist.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category("Боссы")
public class ConfigBosses {
    @ConfigOption.Hidden
    @ConfigEntry(id = "bossWidgetEnabled", translation = "evoassist.ui.bossWidget.title")
    public static boolean widgetEnabled = false;

    @ConfigOption.Hidden
    @ConfigEntry(id = "bossTokensWidgetEnabled", translation = "evoassist.ui.bossTokensWidget.title")
    public static boolean tokensWidgetEnabled = false;
}
