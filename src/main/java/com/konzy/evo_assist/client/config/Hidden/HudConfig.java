package com.konzy.evo_assist.client.config.Hidden;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category(value = "Debug")
public class HudConfig {
    @ConfigOption.Hidden
    @ConfigEntry(id = "chatTabsWidgetX", translation = "chat tabs X")
    public static int ChatTabsX = 2;
    @ConfigOption.Hidden
    @ConfigEntry(id = "chatTabsWidgetY", translation = "chat tabs Y")
    public static int ChatTabsY = -1;
    @ConfigOption.Hidden
    @ConfigEntry(id = "chatTabsWidgetScale", translation = "chat tabs scale")
    public static double ChatTabsScale = 1;
    @ConfigOption.Hidden
    @ConfigEntry(id = "bossWidgetX", translation = "boss widget X")
    public static int BossX = 0;

    @ConfigOption.Hidden
    @ConfigEntry(id = "bossWidgetY", translation = "boss widget Y")
    public static int BossY = 206;

    @ConfigOption.Hidden
    @ConfigEntry(id = "bossWidgetScale", translation = "boss widget scale")
    public static double BossScale = 1;

    @ConfigOption.Hidden
    @ConfigEntry(id = "clanWidgetX", translation = "clan widget X")
    public static int ClanX = 0;

    @ConfigOption.Hidden
    @ConfigEntry(id = "clanWidgetY", translation = "clan widget Y")
    public static int ClanY = 248;

    @ConfigOption.Hidden
    @ConfigEntry(id = "clanWidgetScale", translation = "clan widget scale")
    public static double ClanScale = 1;

    @ConfigOption.Hidden
    @ConfigEntry(
            id = "bphWidgetX",
            translation = "bph widget X"
    )
    public static int WidgetBphX = 0;

    @ConfigOption.Hidden
    @ConfigEntry(
            id = "bphWidgetY",
            translation = "bph widget Y"
    )
    public static int WidgetBphY = 139;

    @ConfigOption.Hidden
    @ConfigEntry(
            id = "bphWidgetScale",
            translation = "bph widget scale"
    )
    public static double WidgetBphScale = 1;

    @ConfigOption.Hidden
    @ConfigEntry(id = "blockGoalWidgetX", translation = "block goal widget X")
    public static int BlockGoalX = 0;

    @ConfigOption.Hidden
    @ConfigEntry(id = "blockGoalWidgetY", translation = "block goal widget Y")
    public static int BlockGoalY = 47;

    @ConfigOption.Hidden
    @ConfigEntry(id = "blockGoalWidgetScale", translation = "block goal widget scale")
    public static double BlockGoalScale = 1;

    @ConfigOption.Hidden
    @ConfigEntry(id = "timeGoalWidgetX", translation = "time goal widget X")
    public static int TimeGoalX = 0;

    @ConfigOption.Hidden
    @ConfigEntry(id = "timeGoalWidgetY", translation = "time goal widget Y")
    public static int TimeGoalY = 92;

    @ConfigOption.Hidden
    @ConfigEntry(id = "timeGoalWidgetScale", translation = "time goal widget scale")
    public static double TimeGoalScale = 1;

    @ConfigOption.Hidden
    @ConfigEntry(id = "goalNoticeWidgetX", translation = "goal notification X")
    // -1 on both axes keeps the default notification centered at any window size.
    public static int GoalNoticeX = -1;

    @ConfigOption.Hidden
    @ConfigEntry(id = "goalNoticeWidgetY", translation = "goal notification Y")
    public static int GoalNoticeY = -1;

    @ConfigOption.Hidden
    @ConfigEntry(id = "goalNoticeWidgetScale", translation = "goal notification scale")
    public static double GoalNoticeScale = 1.5;

    @ConfigOption.Hidden
    @ConfigEntry(id = "moneyGoalX", translation = "money goal X")
    public static int MoneyGoalX = 245;
    @ConfigOption.Hidden
    @ConfigEntry(id = "moneyGoalY", translation = "money goal Y")
    public static int MoneyGoalY = 47;
    @ConfigOption.Hidden
    @ConfigEntry(id = "moneyGoalScale", translation = "money goal scale")
    public static double MoneyGoalScale = 1;
    @ConfigOption.Hidden
    @ConfigEntry(id = "shardGoalX", translation = "shard goal X")
    public static int ShardGoalX = 245;
    @ConfigOption.Hidden
    @ConfigEntry(id = "shardGoalY", translation = "shard goal Y")
    public static int ShardGoalY = 92;
    @ConfigOption.Hidden
    @ConfigEntry(id = "shardGoalScale", translation = "shard goal scale")
    public static double ShardGoalScale = 1;
    @ConfigOption.Hidden
    @ConfigEntry(id = "clanPointsGoalX", translation = "clan points goal X")
    public static int ClanPointsGoalX = 245;
    @ConfigOption.Hidden
    @ConfigEntry(id = "clanPointsGoalY", translation = "clan points goal Y")
    public static int ClanPointsGoalY = 140;
    @ConfigOption.Hidden
    @ConfigEntry(id = "clanPointsGoalScale", translation = "clan points goal scale")
    public static double ClanPointsGoalScale = 1;
    @ConfigOption.Hidden
    @ConfigEntry(id = "clanGoldGoalX", translation = "clan gold goal X")
    public static int ClanGoldGoalX = 245;
    @ConfigOption.Hidden
    @ConfigEntry(id = "clanGoldGoalY", translation = "clan gold goal Y")
    public static int ClanGoldGoalY = 164;
    @ConfigOption.Hidden
    @ConfigEntry(id = "clanGoldGoalScale", translation = "clan gold goal scale")
    public static double ClanGoldGoalScale = 1;
    @ConfigOption.Hidden
    @ConfigEntry(id = "clanExperienceGoalX", translation = "clan experience goal X")
    public static int ClanExperienceGoalX = 245;
    @ConfigOption.Hidden
    @ConfigEntry(id = "clanExperienceGoalY", translation = "clan experience goal Y")
    public static int ClanExperienceGoalY = 188;
    @ConfigOption.Hidden
    @ConfigEntry(id = "clanExperienceGoalScale", translation = "clan experience goal scale")
    public static double ClanExperienceGoalScale = 1;

}
