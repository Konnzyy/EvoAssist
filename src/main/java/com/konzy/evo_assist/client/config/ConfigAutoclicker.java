/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.config;

import static com.konzy.evo_assist.client.util.Texts.tr;
import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category(value = "Автокликер")
public class ConfigAutoclicker {
    @ConfigEntry(id = "autoclickerEnabled", translation = "evoassist.ui.clickerEnabled.title")
    public static boolean autoclickerEnabled = true;

    @ConfigOption.Hidden
    @ConfigEntry(
            id = "autoclickerToggle",
            translation = "evoassist.config.autoclicker.toggle"
    )
    @Comment(
            value = "Не рекомендуется включать, заставляет автокликер работать без вашего участия"
    )
    public static boolean autoclickerToggle = false;


    @ConfigEntry(
            id = "autoclickerButton",
            translation = "evoassist.config.autoclicker.button"
    )
    @ConfigOption.Select
    public static ENUMAutoClickerButton autoclickerButton = ENUMAutoClickerButton.LMB;
    public enum ENUMAutoClickerButton {
        LMB, RMB;
        @Override
        public String toString() {
            return switch (this) {
                case LMB -> tr("evoassist.ui.leftMouse");
                case RMB -> tr("evoassist.ui.rightMouse");
            };
        }}


    @ConfigEntry(
            id = "autoclickerActivation",
            translation = "evoassist.config.autoclicker.activation"
    )
    @ConfigOption.Select
    public static ENUMAutoClickerActivation autoclickerActivation = ENUMAutoClickerActivation.SWITCH;
    public enum ENUMAutoClickerActivation {
        HOLD, SWITCH;
        @Override
        public String toString() {
            return switch (this) {
                case HOLD -> tr("evoassist.ui.holdMode");
                case SWITCH -> tr("evoassist.ui.clickMode");
            };
        }
    }


    @ConfigEntry(
            id = "autoclickerCps",
            translation = "evoassist.config.autoclicker.cps"
    )
    @ConfigOption.Range(min = 1, max = 20)
    @ConfigOption.Slider
    public static int autoclickerCps = 10;
}
