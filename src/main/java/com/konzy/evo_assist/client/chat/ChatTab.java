/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.chat;

import static com.konzy.evo_assist.client.util.Texts.tr;
import java.util.List;
import net.minecraft.network.chat.Component;

public record ChatTab(String name, List<String> startsWithFilters, int color, int hoveredColor, int selectedColor, int bannedColor) {

    @Override
    public String name() { return name.startsWith("evoassist.") ? tr(name) : name; }

    public boolean matches(Component message) {
        if (startsWithFilters.isEmpty()) {
            return true;
        }
        String content = message.getString();
        for (String filter : startsWithFilters) {
            if (content.startsWith(filter)) {
                return true;
            }
        }
        return false;
    }
}
