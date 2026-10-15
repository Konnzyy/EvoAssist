package com.konzy.evo_assist.client.compat.evoplus;

import static com.konzy.evo_assist.client.util.Texts.tr;

import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import com.konzy.evo_assist.client.util.TimeUtils;
import java.math.BigDecimal;

/** Exact snapshots of the data a reset will clear. No abbreviated or rounded amounts. */
public final class ResetDetails {
    private ResetDetails() {}

    public static String amount(BigDecimal value) {
        String[] parts = value.stripTrailingZeros().toPlainString().split("\\.", 2);
        return parts[0].replaceAll("(?<=\\d)(?=(\\d{3})+$)", " ")
                + (parts.length > 1 ? "." + parts[1] : "");
    }

    public static String value(String key, long value) { return value(key, BigDecimal.valueOf(value)); }

    public static String value(String key, BigDecimal value) { return tr(key) + amount(value); }

    public static String mining(long blocks, long millis, BigDecimal money, long shards) {
        return value("evoassist.hud.blocks", blocks) + "\n"
                + tr("evoassist.hud.time") + TimeUtils.asTextTime(millis) + "\n"
                + value("evoassist.hud.money", money) + "\n"
                + value("evoassist.hud.shards", shards);
    }

    public static String goal(MiningGoals.Goal goal, boolean blocks) {
        String progress = blocks ? amount(BigDecimal.valueOf(goal.blocks)) : TimeUtils.asTextTime(goal.activeMillis);
        String target = blocks ? amount(BigDecimal.valueOf(goal.target())) : TimeUtils.asTextTime(goal.target() * 60_000L);
        return tr("evoassist.ui.currentProgress") + " " + progress + " / " + target + "\n"
                + mining(goal.blocks, goal.activeMillis, BigDecimal.valueOf(goal.money), goal.shards);
    }

    public static String goal(AdditionalGoals.Goal goal, AdditionalGoals.Type type) {
        String result = tr("evoassist.ui.currentProgress") + " " + amount(goal.progress) + " / " + amount(goal.target);
        return type.clan ? result : result + "\n" + mining(goal.blocks, goal.activeMillis, goal.money, goal.shards);
    }
}
