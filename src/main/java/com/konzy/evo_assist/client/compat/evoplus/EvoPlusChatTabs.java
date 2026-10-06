package com.konzy.evo_assist.client.compat.evoplus;

import com.konzy.evo_assist.client.chat.ChatPrefix;
import com.konzy.evo_assist.client.chat.ChatTabManager;
import com.konzy.evo_assist.client.chat.ChatTabsHitArea;
import com.konzy.evo_assist.client.chat.ChatTabsLayout;
import com.konzy.evo_assist.client.config.ConfigChat;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import ru.dargen.evoplus.api.render.RenderContext;
import ru.dargen.evoplus.api.render.WidgetRenderer;
import ru.dargen.evoplus.api.setting.SettingContainer;
import ru.dargen.evoplus.api.setting.type.WidgetElement;

/** EvoPlus owns placement; Minecraft's chat screen handles the panel's clicks. */
public final class EvoPlusChatTabs implements WidgetRenderer {
    private record Frame(ChatScreen screen, int width, int height, ChatTabsHitArea area) {}
    private final WidgetElement element;
    private Frame frame;

    public EvoPlusChatTabs(SettingContainer category, boolean migrate, boolean enabled) {
        element = category.widget("chat_tabs", "evoassist.ui.chatTabs.title", 240, 12, this)
                .description("evoassist.ui.chatTabs.hint");
        if (migrate) element.setEnabled(enabled);
    }

    public boolean isEnabled() { return element.isEnabled(); }

    public void invalidateHitArea() { frame = null; }

    @Override
    public void render(RenderContext context) {
        Minecraft client = Minecraft.getInstance();
        boolean editing = context.isEditing();
        var screen = client.gui.screen();
        if (!editing && !(screen instanceof ChatScreen)) {
            frame = null;
            return;
        }
        var manager = ChatTabManager.getInstance();
        var tabs = manager.getTabs();
        var prefixes = ChatPrefix.getAll();
        var layout = new ChatTabsLayout(tabs.stream().map(tab -> context.textWidth(tab.name()) + 6).toList(),
                prefixes.stream().map(prefix -> context.textWidth(prefix.getText()) + 6).toList(),
                ConfigChat.chatTabsOrientation == ConfigChat.Orientation.VERTICAL);
        element.size(layout.width(), layout.height());
        // The public vanilla bridge supplies the final position and scale applied by EvoPlus.
        // Retain only geometry and screen identity, never a RenderContext or graphics instance.
        context.<GuiGraphicsExtractor>vanilla(graphics -> {
            if (client.gui.screen() != screen) return;
            var pose = graphics.pose();
            var area = new ChatTabsHitArea(layout, pose.m00(), pose.m01(), pose.m10(),
                    pose.m11(), pose.m20(), pose.m21());
            var window = client.getWindow();
            ChatTabsLayout.Cell hovered = editing ? null : area.at(
                    client.mouseHandler.getScaledXPos(window), client.mouseHandler.getScaledYPos(window));
            if (!editing) frame = new Frame((ChatScreen) screen, window.getGuiScaledWidth(),
                    window.getGuiScaledHeight(), area);
            for (var cell : layout.cells()) {
                boolean hover = cell.equals(hovered);
                boolean selected;
                String label;
                int color;
                if (cell.prefix()) {
                    var prefix = prefixes.get(cell.index());
                    selected = manager.getCurrentPrefix() == prefix;
                    label = prefix.getText();
                    color = selected ? 0xAAAAAA : hover ? 0xFFFF55 : 0xFFFFFF;
                } else {
                    var tab = tabs.get(cell.index());
                    selected = manager.getActiveTab() == tab;
                    label = tab.name();
                    color = selected ? tab.selectedColor() : manager.isBanned(tab) ? tab.bannedColor()
                            : hover ? tab.hoveredColor() : tab.color();
                }
                int x = cell.x(), y = cell.y(), right = x + cell.width(), bottom = y + cell.height();
                int border = selected ? 0xFF5ACAD5 : hover ? 0xFFB8D5DA : 0x805F7580;
                graphics.fill(x, y, right, bottom, hover ? 0xB0354550 : 0x80000000);
                graphics.fill(x, y, right, y + 1, border);
                graphics.fill(x, bottom - 1, right, bottom, border);
                graphics.fill(x, y, x + 1, bottom, border);
                graphics.fill(right - 1, y, right, bottom, border);
                graphics.centeredText(client.font, label, x + cell.width() / 2, y + 2, color | 0xFF000000);
            }
        });
    }

    public boolean mouseClicked(ChatScreen screen, MouseButtonEvent event, Consumer<ChatPrefix> onPrefix) {
        var client = Minecraft.getInstance();
        var window = client.getWindow();
        Frame current = frame;
        if (!isEnabled() || event.button() != 0 || client.gui.screen() != screen || current == null
                || current.screen() != screen || current.width() != window.getGuiScaledWidth()
                || current.height() != window.getGuiScaledHeight()) return false;
        var cell = current.area().at(event.x(), event.y());
        if (cell == null) return false;
        var manager = ChatTabManager.getInstance();
        if (cell.prefix()) {
            var prefix = ChatPrefix.getAll().get(cell.index());
            manager.setPrefix(prefix);
            onPrefix.accept(prefix);
        } else {
            var tab = manager.getTabs().get(cell.index());
            if (client.hasShiftDown()) manager.setBannedTab(tab);
            else manager.setActiveTab(tab);
        }
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }
}
