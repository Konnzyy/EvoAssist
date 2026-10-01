package com.konzy.evo_assist.client.util;

public class MathUtils {
    public static int map(int x1, int y1, int x2, int y2, int value) {
        return (int) (x2 + Math.round((1.0 * (y2 - x2) / (y1 - x1)) * (value - x1)));
    }
}
