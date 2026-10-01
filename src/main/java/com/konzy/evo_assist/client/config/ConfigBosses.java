package com.konzy.evo_assist.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;

@Category("Боссы")
public class ConfigBosses {
    @ConfigEntry(id = "bossWidgetEnabled", translation = "evoassist.ui.bossWidget.title")
    public static boolean widgetEnabled = false;
}
