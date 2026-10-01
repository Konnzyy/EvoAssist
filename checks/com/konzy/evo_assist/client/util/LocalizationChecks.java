package com.konzy.evo_assist.client.util;

import java.util.Map;

public final class LocalizationChecks {
    private static void equal(String actual, String expected) {
        if (!actual.equals(expected)) throw new AssertionError("Expected '" + expected + "', got '" + actual + "'");
    }

    public static void main(String[] args) {
        // The default translator loads the same English catalog bundled for Minecraft.
        equal(Texts.tr("evoassist.ui.moneyGoal.hint"), "Enter a money goal, for example 5.32B");
        equal(Texts.tr("evoassist.ui.menuKey.title"), "Open menu key");
        equal(Texts.tr("evoassist.notice.blocks", "10 000"), "Goal reached: 10 000 blocks!");
        equal(TimeUtils.asTextTime(0), "0s");
        equal(TimeUtils.asTextTime(79_000), "1min 19s");
        equal(TimeUtils.asTextTime(7_500_000), "2h 5min");
        equal(TimeUtils.asTextTime(93_784_000), "1d 2h 3min 4s");
        equal(TimeUtils.asTextTime(360_000_000), "4d 4h");

        // A language switch must affect subsequent calls, without cached translated units.
        Map<String, String> russian = Map.of("evoassist.time.day", "д", "evoassist.time.hour", "ч",
                "evoassist.time.minute", "мин", "evoassist.time.second", "сек");
        Texts.useTranslator((key, parameters) -> russian.getOrDefault(key, key));
        equal(TimeUtils.asTextTime(0), "0сек");
        equal(TimeUtils.asTextTime(79_000), "1мин 19сек");
        equal(TimeUtils.asTextTime(93_784_000), "1д 2ч 3мин 4сек");
        // Translating visible units must not change the Russian server time parser.
        if (TimeUtils.fromTextTime("2 ч 5 мин 7 сек") != 7_507_000L)
            throw new AssertionError("Server time parsing changed");
        System.out.println("Localization checks passed: fallback, substitutions, live time units and server parser.");
    }
}
