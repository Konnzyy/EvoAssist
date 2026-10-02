package com.konzy.evo_assist.client.compat;

import com.konzy.evo_assist.client.ui.EvoConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class EvoModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<EvoConfigScreen> getModConfigScreenFactory() {
        return EvoConfigScreen::new;
    }
}
