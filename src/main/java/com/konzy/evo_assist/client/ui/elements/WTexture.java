package com.konzy.evo_assist.client.ui.elements;

import net.minecraft.resources.Identifier;

public class WTexture {
    private final Identifier texture;
    private final int x;
    private final int y;
    private final boolean centered;
    private final int widgetWidth;
    private final int widgetHeight;
    private final int scale;
    WTexture(Identifier texture, int x, int y, boolean centered, int widgetWidth, int widgetHeight, int scale) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.centered = centered;
        this.widgetWidth = widgetWidth;
        this.widgetHeight = widgetHeight;
        this.scale = scale;
    }

    public Identifier getTexture() { return texture; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean getCentered() { return centered; }
    public int getWidgetWidth() { return widgetWidth; }
    public int getWidgetHeight() { return widgetHeight; }
    public int getScale() { return scale; }
}
