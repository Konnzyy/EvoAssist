package com.konzy.evo_assist.client.chat;

import java.util.ArrayList;
import java.util.List;

/** Shared geometry for drawing, dragging and clicking chat controls. */
public final class ChatTabsLayout {
    public record Cell(boolean prefix, int index, int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }
    public record Placement(int x, int y, double scale, int width, int height) {}
    private final List<Cell> cells;
    private final int width, height;

    public int height() { return height; }
    public Cell at(double x, double y) {
        return cells.stream().filter(cell -> cell.contains(x, y)).findFirst().orElse(null);
    }
    public List<Cell> cells() { return cells; }
    public int width() { return width; }
    public ChatTabsLayout(List<Integer> tabWidths, List<Integer> prefixWidths, boolean vertical) {
        List<Cell> result = new ArrayList<>();
        int x = 0, y = 0;
        int columnWidth = tabWidths.stream().mapToInt(Integer::intValue).max().orElse(1);
        int columnRows = (tabWidths.size() + 1) / 2;
        for (int i = 0; i < tabWidths.size(); i++) {
            int w = vertical ? columnWidth : tabWidths.get(i);
            if (vertical) {
                result.add(new Cell(false, i, (i / columnRows) * (columnWidth + 2), (i % columnRows) * 14, w, 12));
            } else {
                result.add(new Cell(false, i, x, 0, w, 12));
                x += w + 2;
            }
        }
        if (vertical) { y = tabWidths.isEmpty() ? 0 : columnRows * 14 + 4; }
        else if (!tabWidths.isEmpty() && !prefixWidths.isEmpty()) x += 13;
        for (int i = 0; i < prefixWidths.size(); i++) {
            int w = prefixWidths.get(i);
            result.add(new Cell(true, i, x, y, w, 12));
            x += w + 2;
        }
        cells = List.copyOf(result);
        width = Math.max(1, cells.stream().mapToInt(cell -> cell.x + cell.width).max().orElse(1));
        height = Math.max(1, cells.stream().mapToInt(cell -> cell.y + cell.height).max().orElse(1));
    }

    public Placement place(int requestedX, int requestedY, double requestedScale, int screenWidth, int screenHeight) {
        int availableWidth = Math.max(1, screenWidth - 4);
        int availableHeight = Math.max(1, screenHeight - 16);
        double scale = Double.isFinite(requestedScale) ? Math.clamp(requestedScale, 0.3, 5) : 1;
        scale = Math.min(scale, Math.min(availableWidth / (double) width, availableHeight / (double) height));
        int scaledWidth = Math.clamp((int) Math.ceil(width * scale), 1, availableWidth);
        int scaledHeight = Math.clamp((int) Math.ceil(height * scale), 1, availableHeight);
        int maxX = Math.max(2, screenWidth - scaledWidth - 2);
        int maxY = Math.max(2, screenHeight - scaledHeight - 14);
        return new Placement(Math.clamp(requestedX, 2, maxX),
                requestedY < 0 ? maxY : Math.clamp(requestedY, 2, maxY), scale, scaledWidth, scaledHeight);
    }
}
