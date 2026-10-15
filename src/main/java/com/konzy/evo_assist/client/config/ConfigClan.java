package com.konzy.evo_assist.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category("Клан")
public class ConfigClan {
    @ConfigOption.Hidden
    @ConfigEntry(id = "clanWidgetEnabled", translation = "evoassist.ui.clanWidget.title")
    public static boolean widgetEnabled = false;

    @ConfigEntry(id = "pointsGoalTarget", translation = "evoassist.ui.clanPointsGoal.title")
    public static String pointsGoalTarget = "0";
    @ConfigEntry(id = "goldGoalTarget", translation = "evoassist.ui.clanGoldGoal.title")
    public static String goldGoalTarget = "0";
    @ConfigEntry(id = "experienceGoalTarget", translation = "evoassist.ui.clanExperienceGoal.title")
    public static String experienceGoalTarget = "0";
    @ConfigOption.Hidden
    @ConfigEntry(id = "pointsGoalWidgetEnabled", translation = "evoassist.ui.clanPointsGoalWidget.title")
    public static boolean pointsGoalWidgetEnabled = true;
    @ConfigOption.Hidden
    @ConfigEntry(id = "goldGoalWidgetEnabled", translation = "evoassist.ui.clanGoldGoalWidget.title")
    public static boolean goldGoalWidgetEnabled = true;
    @ConfigOption.Hidden
    @ConfigEntry(id = "experienceGoalWidgetEnabled", translation = "evoassist.ui.clanExperienceGoalWidget.title")
    public static boolean experienceGoalWidgetEnabled = true;
    @ConfigEntry(id = "goalNotifications", translation = "evoassist.ui.goalNotifications.title")
    public static boolean goalNotifications = true;
}
