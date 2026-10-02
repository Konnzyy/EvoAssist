/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.ui.elements;

import net.minecraft.network.chat.Component;

public class WText {
    private final Component text;
    private final int color;
    private int x;
    private int y;
    private final boolean padding;
    WText(Component text, int color, int x, int y, boolean padding) {
        this.text = text;
        this.color = color;
        this.x = x;
        this.y = y;
        this.padding = padding;
    }
    public Component getText() {
        return text;
    }
    public int getColor() {
        return color;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public boolean padding() {
        return padding;
    }
}
