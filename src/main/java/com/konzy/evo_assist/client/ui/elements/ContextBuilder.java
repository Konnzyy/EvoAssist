package com.konzy.evo_assist.client.ui.elements;

import com.konzy.evo_assist.client.EvoAssistClient;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ContextBuilder {
    private final List<WTexture> texturesList;
    private final List<WText> textList;
    private final int padding;
    private int x;
    private int y;
    private final int height;

    private final String[] cachedLines;
    private final int[] cachedColors;


    ContextBuilder(Builder builder) {
        this.texturesList = new ArrayList<>(builder.texturesList);
        this.textList = new ArrayList<>(builder.textList);

        this.padding = builder.padding;

        this.x = builder.x;
        this.y = builder.y;

        this.height = builder.height;


        this.cachedLines = new String[textList.size()];
        this.cachedColors = new int[textList.size()];
        Identifier[] cachedIcons = new Identifier[texturesList.size()];


        for(int i = 0; i < textList.size(); i++) {
            WText text = textList.get(i);
            cachedLines[i] = text.getText().getString();
            cachedColors[i] = text.getColor();
        }
        for(int i = 0; i < texturesList.size(); i++) {
            WTexture wtexture = texturesList.get(i);
            cachedIcons[i] = wtexture.getTexture();
        }

    }


    public void updateLine(int index, Component newText) {
        if(index >= 0 && index < textList.size()) {
            String newString = newText.getString();
            if(!cachedLines[index].equals(newString)) {
                cachedLines[index] = newString;

                textList.set(index, new WText(
                        Component.literal(newString),
                        cachedColors[index],
                        textList.get(index).getX(),
                        textList.get(index).getY(),
                        textList.get(index).padding()
                ));
            }
        }
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }



    public void draw(GuiGraphicsExtractor context) {
        for(WTexture icon : texturesList) {
            if (icon != null) {
                renderTexture(context, icon);
            }
        }
        int currentPadding = 0;
        for(WText text : textList) {
            int textX = x + text.getX();
            int textY = y + text.getY() + (text.padding() ? currentPadding : 0);
            context.text(EvoAssistClient.instance.font, Component.literal(cachedLines[textList.indexOf(text)]),
                    textX, textY, cachedColors[textList.indexOf(text)] | 0xFF000000, true);

            if(text.padding()) {
                currentPadding += padding;
            }
        }

    }

    private void renderTexture(GuiGraphicsExtractor context, WTexture icon) {
        int iconScale = icon.getScale();
        if(icon.getCentered()) {
            int ypos = height / 2 - iconScale / 2;
            context.blit(RenderPipelines.GUI_TEXTURED,
                    icon.getTexture(),
                    x + icon.getX(),
                    y + ypos,
                    0, 0, iconScale, iconScale, iconScale, iconScale, 0xFFFFFFFF);
        } else {
            context.blit(RenderPipelines.GUI_TEXTURED,
                    icon.getTexture(),
                    x + icon.getX(),
                    y + icon.getY(),
                    0, 0, iconScale, iconScale, iconScale, iconScale, 0xFFFFFFFF);
        }
    }


    public static class Builder {
        private final ArrayList<WTexture> texturesList = new ArrayList<>();
        private final ArrayList<WText> textList = new ArrayList<>();
        private int padding;
        private int x;
        private int y;
        private int height;

        public Builder addTexture(Identifier texture, int x, int y, boolean centered, int widgetWidth, int widgetHeight, int scale) {
            this.texturesList.add(new WTexture(texture, x, y, centered, widgetWidth, widgetHeight, scale));
            return this;
        }

        public Builder addLine(Component text, int color, int x, int y, boolean usePadding) {
            this.textList.add(new WText(text, color, x, y, usePadding));
            return this;
        }

        public Builder setPadding(int padding) {
            this.padding = padding;
            return this;
        }

        public Builder setX(int x) {
            this.x = x;
            return this;
        }

        public Builder setY(int y) {
            this.y = y;
            return this;
        }

        public Builder setWidth(int width) {
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setScale(int scale) {
            return this;
        }

        public ContextBuilder build() {
            return new ContextBuilder(this);
        }
    }

}
