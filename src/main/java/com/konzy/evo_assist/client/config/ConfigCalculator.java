package com.konzy.evo_assist.client.config;

import com.konzy.evo_assist.client.features.calculator.LevelCalculator;
import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category("Калькулятор")
public class ConfigCalculator {
    @ConfigOption.Hidden
    @ConfigEntry(id = "mode", translation = "evoassist.calculator.mode")
    public static LevelCalculator.Mode mode = LevelCalculator.Mode.EVO;
    @ConfigOption.Hidden
    @ConfigEntry(id = "currentLevel", translation = "evoassist.calculator.currentLevel")
    public static String currentLevel = "";
    @ConfigOption.Hidden
    @ConfigEntry(id = "targetLevel", translation = "evoassist.calculator.targetLevel")
    public static String targetLevel = "";
    @ConfigOption.Hidden
    @ConfigEntry(id = "discount", translation = "evoassist.calculator.discount")
    public static int discount = 0;
}
