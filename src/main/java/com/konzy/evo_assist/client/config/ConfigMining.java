/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.config;

import static com.konzy.evo_assist.client.util.Texts.tr;
import com.konzy.evo_assist.client.features.mine.blockPH.BlockProfitPerHour;
import com.teamresourceful.resourcefulconfig.api.annotations.*;

@Category("Шахта")
public class ConfigMining {

    @ConfigOption.Separator(
            value = "§bСчетчик денег в час",
            description = "Считает примерное количество блоков/денег в час"
    )
    @ConfigEntry(
            id = "bphWidgetToggle",
            translation = "evoassist.ui.miningWidget.title"
    )
    public static boolean bphWidgetToggle = false;


    @ConfigEntry(
            id = "bphWidgetAllowed",
            translation = "evoassist.ui.allowed.title"
    )
    @ConfigOption.Select("Выбрать")
    public static bphAllowEnum[] bphWidgetAllowed = bphAllowEnum.values();

    public enum bphAllowEnum {
        BLOCKS, BARRELS, RUNES, BOMBS, PETS, WANDS, MULTITOOL;

        @Override
        public String toString() {
            return switch (this) {
                case BLOCKS -> tr("evoassist.ui.blocks");
                case BARRELS -> tr("evoassist.ui.barrels");
                case RUNES -> tr("evoassist.ui.runes");
                case BOMBS -> tr("evoassist.ui.bombs");
                case PETS -> tr("evoassist.ui.pets");
                case WANDS -> tr("evoassist.ui.wands");
                case MULTITOOL -> tr("evoassist.ui.multitool");
            };
        }

    }



    @ConfigButton(text = "Сбросить", title = "Сбросить виджет")
    public static final Runnable bphWidgetReset = () -> {
        BlockProfitPerHour.getInstance().reset();
    };

    @ConfigEntry(id = "blockGoalTarget", translation = "evoassist.ui.blockGoal.title")
    public static int blockGoalTarget = 0;

    @ConfigEntry(id = "timeGoalMinutes", translation = "evoassist.ui.timeGoal.title")
    public static int timeGoalMinutes = 0;

    @ConfigEntry(id = "blockGoalWidgetEnabled", translation = "evoassist.ui.blockGoalWidget.title")
    public static boolean blockGoalWidgetEnabled = true;

    @ConfigEntry(id = "timeGoalWidgetEnabled", translation = "evoassist.ui.timeGoalWidget.title")
    public static boolean timeGoalWidgetEnabled = true;

    @ConfigEntry(id = "moneyGoalTarget", translation = "evoassist.ui.moneyGoal.title")
    public static String moneyGoalTarget = "0";
    @ConfigEntry(id = "shardGoalTarget", translation = "evoassist.ui.shardGoal.title")
    public static String shardGoalTarget = "0";
    @ConfigEntry(id = "moneyGoalWidgetEnabled", translation = "evoassist.ui.moneyGoalWidget.title")
    public static boolean moneyGoalWidgetEnabled = true;
    @ConfigEntry(id = "shardGoalWidgetEnabled", translation = "evoassist.ui.shardGoalWidget.title")
    public static boolean shardGoalWidgetEnabled = true;

    @ConfigEntry(id = "goalNotifications", translation = "evoassist.ui.goalNotifications.title")
    public static boolean goalNotifications = true;

}
