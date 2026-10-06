package com.konzy.evo_assist.client.chat;

/** A snapshot of the transform EvoPlus used to draw the clickable chat panel. */
public record ChatTabsHitArea(ChatTabsLayout layout, double m00, double m01, double m10,
                              double m11, double m20, double m21) {
    public ChatTabsLayout.Cell at(double screenX, double screenY) {
        double determinant = m00 * m11 - m01 * m10;
        if (!Double.isFinite(determinant) || Math.abs(determinant) < 1e-9) return null;
        double x = screenX - m20, y = screenY - m21;
        double localX = (m11 * x - m10 * y) / determinant;
        double localY = (m00 * y - m01 * x) / determinant;
        if (!Double.isFinite(localX) || !Double.isFinite(localY)) return null;
        return layout.at(localX, localY);
    }
}
