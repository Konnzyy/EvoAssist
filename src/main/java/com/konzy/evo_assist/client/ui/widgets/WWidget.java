package com.konzy.evo_assist.client.ui.widgets;

import com.konzy.evo_assist.client.Evo_assistClient;
import com.konzy.evo_assist.client.ui.WidgetScreen;
import com.konzy.evo_assist.client.ui.elements.ContextBuilder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

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
    public void hideHud(boolean value) {
        hudHidden = value;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if(hidden) return;
        super.onClick(event, doubleClick);
        isDragging = true;
    }


    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if(hidden) return;
        if(hudHidden && widgetScreen == null) return;
        if(widgetScreen != null) {
            context.text(Evo_assistClient.instance.font, Component.literal(String.format("x%s", (double) Math.round(scale * 10) / 10)),
                    getX(), getY() - 10, 0xFFFFFFFF, true); // отображение размера виджета atm
        }

        this.width = (int) (baseWidth * scale);
        this.height = (int) (baseHeight * scale); // устанавливается размер ВИДЖЕТА(ClickableWidget), АКА кликабельная зона

        context.enableScissor(getX(), getY(), getX() + width, getY() + height);

        context.pose().pushMatrix();
        context.pose().translate(-((float) scale-1) * getX(), -((float) scale-1) * getY());
        context.pose().scale((float) scale, (float) scale);

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


        //
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        if(hidden) return;
        this.x += deltaX;
        this.y += deltaY;
        if(widgetScreen != null) {
            this.x = Math.max(0, Math.min(this.x, widgetScreen.width - width));
            this.y = Math.max(0, Math.min(this.y, widgetScreen.height - height));
        }
        setX((int) Math.round(this.x));
        setY((int) Math.round(this.y));
    }

    protected abstract void savePosition();

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}

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
    protected ContextBuilder getContextBuilder() {
        return contextBuilder;
    }
    protected void updateLine(int index, Component newText) {
        if(contextBuilder != null) {
            contextBuilder.updateLine(index, newText);
        }
    }
    protected void updateColor(int index, int newColor) {
        if(contextBuilder != null) {
            contextBuilder.updateColor(index, newColor);
        }
    }
    protected void updateIcon(int index, Identifier icon) {
        if(contextBuilder != null) {
            contextBuilder.updateIcon(index, icon);
        }
    }
    protected void updateBar(int index, int value, int min, int max) {
        if(contextBuilder != null) {
            contextBuilder.updateBar(index, value, min, max);
        }
    }

    public void resize(double amount) {
        float delta = (float) Math.signum(amount) * 0.1f; //0.1ф - шаг
        scale = loadScale();
        double newScale = Math.max(0.3, Math.min(5.0, scale + delta));
        if(newScale != scale) {
            scale = newScale;
            saveScale(newScale);
        }
    }

    protected abstract void saveScale(double scale);
    protected abstract double loadScale();
    public void setScale(double scale) { this.scale = scale; } // !!! ставит скейл для худ виджета, ничего не сохраняет и не подгружает

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }

    protected void setBaseWidth(int width) { this.baseWidth = width; }
    protected void setBaseHeight(int height) { this.baseHeight = height; }




}
