package com.konzy.evo_assist.client.chat;

import java.util.List;

public final class ChatTabsLayoutChecks {
    private static int checks;

    private static void check(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }

    private static void staysOnScreen(ChatTabsLayout layout, int screenWidth, int screenHeight) {
        for (double scale : new double[] {0.3, 1, 1.5, 5, 999, Double.NaN}) {
            for (int x : new int[] {-999, 2, 9999}) {
                for (int y : new int[] {-1, 2, 9999}) {
                    var p = layout.place(x, y, scale, screenWidth, screenHeight);
                    check(p.x() >= 2 && p.y() >= 2
                            && p.x() + p.width() <= screenWidth - 2
                            && p.y() + p.height() <= screenHeight - 14,
                            "Placement must stay on screen and above chat input: " + p);
                    for (var cell : layout.cells()) {
                        double clickX = p.x() + (cell.x() + cell.width() / 2.0) * p.scale();
                        double clickY = p.y() + (cell.y() + cell.height() / 2.0) * p.scale();
                        check(layout.at((clickX - p.x()) / p.scale(), (clickY - p.y()) / p.scale()) == cell,
                                "Scaled click must reach its rendered control");
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        var horizontal = new ChatTabsLayout(List.of(24, 30, 18, 12, 12, 12), List.of(12, 12, 12, 12, 18), false);
        var vertical = new ChatTabsLayout(List.of(24, 30, 18, 12, 12, 12), List.of(12, 12, 12, 12, 18), true);
        check(horizontal.height() == 12, "Horizontal controls occupy one row");
        check(horizontal.cells().stream().allMatch(cell -> cell.y() == 0), "All horizontal controls share a row");
        check(vertical.cells().stream().filter(cell -> !cell.prefix()).allMatch(cell -> cell.width() == 30),
                "Vertical tabs have equal widths");
        check(vertical.cells().get(0).x() == vertical.cells().get(2).x()
                        && vertical.cells().get(3).x() == vertical.cells().get(5).x()
                        && vertical.cells().get(3).x() > vertical.cells().get(0).x(),
                "ALL, CLAN, DM occupy the first column and L, G, M the second");
        check(vertical.cells().get(0).y() == vertical.cells().get(3).y()
                        && vertical.cells().get(1).y() == vertical.cells().get(4).y()
                        && vertical.cells().get(2).y() == vertical.cells().get(5).y(),
                "Both columns align across three rows");
        check(vertical.cells().get(6).y() > vertical.cells().get(5).y() + 12,
                "Prefixes stay below the vertical tab column");
        check(horizontal.at(24, 5) == null && vertical.at(5, 12) == null, "Spacing does not select a tab");
        check(horizontal.at(-1, 0) == null && horizontal.at(horizontal.width(), 0) == null,
                "Clicks outside the widget do nothing");
        for (var layout : List.of(horizontal, vertical)) {
            for (int[] screen : new int[][] {{960, 540}, {320, 180}, {80, 60}, {240, 30}}) {
                staysOnScreen(layout, screen[0], screen[1]);
            }
            var p = layout.place(2, -1, 1, 960, 540);
            check(p.y() + p.height() == 526, "Default position remains anchored above the input");
        }
        check(new ChatTabsLayout(List.of(), List.of(), false).at(0, 0) == null, "Empty layout has no click targets");
        System.out.println("Chat layout checks passed: " + checks);
    }
}
