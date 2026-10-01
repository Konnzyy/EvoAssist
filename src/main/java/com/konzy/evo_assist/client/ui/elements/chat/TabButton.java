package com.konzy.evo_assist.client.ui.elements.chat;

import com.konzy.evo_assist.client.chat.ChatTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class TabButton extends Button {
    private final ChatTab tab;
    private boolean activetab;
    private boolean banned = false;
    private final OnPress leftClick;


    public TabButton(int x, int y, ChatTab tab, OnPress leftClick) {
        super(x, y, 0, 12, Component.literal(tab.name()), leftClick, DEFAULT_NARRATION);
        this.tab = tab;
        this.width = Minecraft.getInstance().font.width(tab.name()) + 6;
        this.leftClick = leftClick;

        this.setFocused(false);
    }

    public ChatTab getTab() { return tab; }
    public void setActive(boolean active) { this.activetab = active; }
    public void setBanned(boolean banned) { this.banned = banned; }

    @Override
    protected void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int textColor = (activetab ?
                tab.selectedColor() : (banned ?
                tab.bannedColor() : (isHovered() ?
                tab.hoveredColor() : tab.color())));

        context.fill(getX(), getY(), getX() + width, getY() + height, 0x80000000);
        Font textRenderer = Minecraft.getInstance().font;
        context.centeredText(textRenderer, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, textColor | 0xFF000000);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.active && this.visible && this.isMouseOver(event.x(), event.y())) {

            if (event.button() == 0 && leftClick != null) { //лкм
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.leftClick.onPress(this);
                return true;
            }
        }
        return false;
    }
}
