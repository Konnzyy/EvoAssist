package com.konzy.evo_assist.client.compat;

import com.konzy.evo_assist.client.compat.evoplus.EvoPlusIntegration;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class EvoModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return parent -> {
            EvoPlusIntegration.openWidgetSettings();
            return Minecraft.getInstance().gui.screen();
        };
    }
}
