package com.konzy.evo_assist.client.config;

import com.konzy.evo_assist.client.Evo_assistClient;
import com.konzy.evo_assist.client.config.Hidden.HudConfig;
import com.konzy.evo_assist.client.ui.WidgetScreen;
import com.teamresourceful.resourcefulconfig.api.annotations.*;

@com.teamresourceful.resourcefulconfig.api.annotations.Config(
        value = Evo_assistClient.MODID,
        categories = {
                ConfigAutoclicker.class,
                ConfigMining.class,
                ConfigBosses.class,
                ConfigClan.class,
                ConfigChat.class,
                ConfigVisual.class,
                HudConfig.class

        }
)
@ConfigInfo(
        icon = "fish",
        title = "§d§lEvoAssist",
        description = "QoL mod for PrisonEvo mode of Diamond World",
        descriptionTranslation = "evoassist.config.info",
        links = {
            @ConfigInfo.Link(
                    value = "https://discord.gg/sx4TXM2NX8",
                    icon = "code-2",
                    text = "Discord"
            )
        }
)
public class Config {

    @Comment(value = "§cДВИГАТЬ ВИДЖЕТЫ ТУТ ----------------------->\n§cДВИГАТЬ ВИДЖЕТЫ ТУТ ----------------------->\n§cДВИГАТЬ ВИДЖЕТЫ ТУТ ----------------------->")
    @ConfigButton(text = "Открыть", title = "Открыть меню редактирования виджетов")
    public static final Runnable editWidgetsButton = () -> {
        Evo_assistClient.instance.gui.setScreen(new WidgetScreen());
    }; /////////////////////////////////////


}
