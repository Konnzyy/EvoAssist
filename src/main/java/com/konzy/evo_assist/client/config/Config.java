/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.config;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.Hidden.HudConfig;
import com.konzy.evo_assist.client.ui.EvoConfigScreen;
import com.teamresourceful.resourcefulconfig.api.annotations.*;

@com.teamresourceful.resourcefulconfig.api.annotations.Config(
        value = EvoAssistClient.MODID,
        categories = {
                ConfigMining.class,
                ConfigBosses.class,
                ConfigClan.class,
                ConfigCalculator.class,
                ConfigChat.class,
                ConfigVisual.class,
                HudConfig.class

        }
)
@ConfigInfo(
        icon = "fish",
        title = "§e§lEvoAssist",
        description = "Client-side QoL tools for DiamondWorld Prison Evo.",
        descriptionTranslation = "evoassist.config.info"
)
public class Config {
    @ConfigButton(text = "Открыть", title = "Открыть настройки EvoAssist")
    public static final Runnable openEvoAssistButton = () -> {
        var client = EvoAssistClient.instance;
        client.gui.setScreen(new EvoConfigScreen(client.gui.screen()));
    };


}
