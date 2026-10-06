/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.compat.evoplus;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import ru.dargen.evoplus.api.render.RenderContext;
import ru.dargen.evoplus.api.render.WidgetRenderer;
import ru.dargen.evoplus.api.setting.SettingContainer;
import ru.dargen.evoplus.api.setting.type.WidgetElement;

/** Draws the entire widget in EvoPlus coordinates, including its icon. */
public final class EvoPlusWidget implements WidgetRenderer {
    public record Content(String texture, int iconSize, List<String> lines) {
        public Content { lines = List.copyOf(lines); }
    }

    private final Supplier<Content> content;
    private final BooleanSupplier available;
    private final WidgetElement element;

    public EvoPlusWidget(SettingContainer category, String id, String name, String description,
                         boolean initializeVisibility, boolean enabled,
                         BooleanSupplier available, Supplier<Content> content) {
        this.content = content;
        this.available = available;
        element = category.widget(id, name, 230, 31, this).description(description);
        // Migrate visibility once; later sessions use EvoPlus's saved value.
        if (initializeVisibility) element.setEnabled(enabled);
    }

    @Override
    public void render(RenderContext context) {
        if (!context.isEditing() && !available.getAsBoolean()) return;
        Content value = content.get();
        int textX = value.iconSize() + 3;
        int lineStep = context.lineHeight() + 1;
        int textWidth = value.lines().stream().mapToInt(context::textWidth).max().orElse(0);
        int height = Math.max(value.iconSize(), value.lines().size() * lineStep) + 2;
        element.size(textX + textWidth + 2, height);
        context.texture(value.texture(), 0, Math.max(0, (height - value.iconSize()) / 2.0),
                value.iconSize(), value.iconSize());
        for (int i = 0; i < value.lines().size(); i++) {
            context.text(value.lines().get(i), textX, 1 + i * lineStep, 0xFFFFFFFF, true);
        }
    }
}
