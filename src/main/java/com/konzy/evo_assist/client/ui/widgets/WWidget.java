/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.ui.widgets;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.ui.WidgetScreen;
import com.konzy.evo_assist.client.ui.elements.ContextBuilder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public abstract class WWidget extends AbstractWidget {
    protected final WidgetScreen widgetScreen;
    protected ContextBuilder contextBuilder;
    protected boolean isDragging = false;

    private double x;
    private double y;
    private int baseWidth;
    private int baseHeight;
    protected boolean hidden;
    protected boolean hudHidden;
    protected double scale;

    protected abstract void saveScale(double scale);
    protected abstract double loadScale();
    public void setScale(double scale) { this.scale = scale; }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }

    protected void setBaseWidth(int width) { this.baseWidth = width; }
    protected void setBaseHeight(int height) { this.baseHeight = height; }

    public WWidget(int x, int y, int width, int height, WidgetScreen widgetScreen, double scale) {
        super(x, y, width, height, Component.empty());
        this.widgetScreen = widgetScreen;
        this.x = x;
        this.y = y;
        this.baseWidth = width;
        this.baseHeight = height;
        this.hidden = true;
        this.hudHidden = false;
        this.scale = scale;
    }

    public void hide(boolean value) {
        hidden = value;
    }

    @Override
    public void onClick(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if(hidden) return;
        super.onClick(event, doubleClick);
        isDragging = true;
    }

    private void beginScaledRender(@NonNull GuiGraphicsExtractor context) {
        context.enableScissor(getX(), getY(), getX() + width, getY() + height);
        context.pose().pushMatrix();

        double offset = 1.0 - scale;
        context.pose().translate((float) (offset * getX()), (float) (offset * getY()));
        context.pose().scale((float) scale, (float) scale);
    }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if(hidden) return;
        if(hudHidden && widgetScreen == null) return;
        if(widgetScreen != null) {
            context.text(EvoAssistClient.instance.font, Component.literal(String.format("x%s", (double) Math.round(scale * 10) / 10)),
                    getX(), getY() - 10, 0xFFFFFFFF, true);
        }

        this.width = (int) (baseWidth * scale);
        this.height = (int) (baseHeight * scale);

        beginScaledRender(context);

        int rx = (int) Math.round(x);
        int ry = (int) Math.round(y);

        this.setX(rx);
        this.setY(ry);

        renderBg(context);
        if(contextBuilder != null) {
            contextBuilder.setPosition(rx, ry);
            contextBuilder.draw(context);

        }

        context.pose().popMatrix();

        context.disableScissor();

    }

    @Override
    protected void onDrag(@NonNull MouseButtonEvent event, double deltaX, double deltaY) {
        if(hidden) return;
        this.x += deltaX;
        this.y += deltaY;
        if(widgetScreen != null) {
            this.x = Math.clamp(this.x, 0, Math.max(0, widgetScreen.width - width));
            this.y = Math.clamp(this.y, 0, Math.max(0, widgetScreen.height - height));
        }
        setX((int) Math.round(this.x));
        setY((int) Math.round(this.y));
    }

    protected abstract void savePosition();

    @Override
    protected void updateWidgetNarration(@NonNull NarrationElementOutput builder) {}

    protected void renderBg(GuiGraphicsExtractor context) {
        if(hidden) return;
        if(widgetScreen == null) return;
        int bgColor = isHovered() ? 0x80bebebe : 0x808a8a8a;
        context.fill(getX(), getY(), getX()+baseWidth, getY()+baseHeight, bgColor);
    }


    public void applyPos() {
        this.x = getX();
        this.y = getY();
        if(contextBuilder != null) {
            contextBuilder.setPosition(getX(), getY());
        }
    }
    protected void updateLine(int index, Component newText) {
        if(contextBuilder != null) {
            contextBuilder.updateLine(index, newText);
        }
    }

    public void resize(double amount) {
        float delta = (float) Math.signum(amount) * 0.1f;
        scale = loadScale();
        double newScale = Math.clamp(scale + delta, 0.3, 5.0);
        if(newScale != scale) {
            scale = newScale;
            saveScale(newScale);
        }
    }
}
