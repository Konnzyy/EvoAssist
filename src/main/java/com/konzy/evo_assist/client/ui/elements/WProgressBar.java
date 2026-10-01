package com.konzy.evo_assist.client.ui.elements;

import com.konzy.evo_assist.client.util.MathUtils;

public class WProgressBar {
    private int x;
    private int y;
    private final int width;
    private final int height;
    private int backColor;
    private int frontColor;
    private int value;

    public WProgressBar(int x, int y, int width, int height, int backColor, int frontColor) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.backColor = backColor;
        this.frontColor = frontColor;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBackColor() {
        return backColor;
    }

    public int getFrontColor() {
        return frontColor;
    }
    public int getValue() {
        return value;
    }
    public void setValue(int x1, int y1, int value) {
        this.value = MathUtils.map(x1, y1, 1, width, value);
    }

    public void setBackColor(int color) {
        this.backColor = color;
    }
    public void setFrontColor(int color) {
        this.frontColor = color;
    }
    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
