package com.konzy.evo_assist.client.ui.elements.chat;

import com.konzy.evo_assist.client.chat.ChatPrefix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class PrefixButton extends Button {
    private final ChatPrefix prefix;
    private boolean active;

    public PrefixButton(int x, int y, ChatPrefix prefix, OnPress onPress) {
        super(x, y, 0, 12, Component.literal(prefix.getText()), onPress, DEFAULT_NARRATION);
        this.prefix = prefix;
        this.width = Minecraft.getInstance().font.width(prefix.getText()) + 6;
    }

    public ChatPrefix getPrefix() { return prefix; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int color = active ? 0xAAAAAA : (isHovered() ? 0xFFFF55 : 0xFFFFFF);
        context.fill(getX(), getY(), getX() + width, getY() + height, 0x80000000);
        Font textRenderer = Minecraft.getInstance().font;
        context.centeredText(textRenderer, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, color | 0xFF000000);
    }
}
