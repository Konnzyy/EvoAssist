package com.konzy.evo_assist.client.ui.widgets;

import static com.konzy.evo_assist.client.util.Texts.tr;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.Hidden.HudConfig;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import com.konzy.evo_assist.client.ui.WidgetScreen;
import com.konzy.evo_assist.client.ui.elements.ContextBuilder;
import com.konzy.evo_assist.client.util.MoneyUtils;
import com.konzy.evo_assist.client.util.TimeUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;


public final class WiMiningGoal extends WWidget {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("minecraft", "textures/item/netherite_pickaxe.png");
    private final boolean blockGoal;

    public WiMiningGoal(boolean blockGoal, int x, int y, WidgetScreen editor) {
        super(x, y, 230, blockGoal ? 31 : 22, editor,
                blockGoal ? HudConfig.BlockGoalScale : HudConfig.TimeGoalScale);
        this.blockGoal = blockGoal;
    }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        MiningGoals goals = MiningGoals.getInstance();
        MiningGoals.Goal goal = blockGoal ? goals.blockGoal() : goals.timeGoal();
        boolean widgetEnabled = blockGoal ? ConfigMining.blockGoalWidgetEnabled : ConfigMining.timeGoalWidgetEnabled;
        if (widgetScreen == null && (!goal.active() || !widgetEnabled)) {
            hide(true);
            return;
        }
        hide(false);
        if (contextBuilder == null) {
            ContextBuilder.Builder builder = new ContextBuilder.Builder()
                    .addTexture(ICON, 0, 0, true, width, height, 20)
                    .setPadding(10)
                    .addLine(Component.literal(""), 0xFFFFFFFF, 22, 1, true);
            if (blockGoal) builder.addLine(Component.literal(""), 0xFFFFFFFF, 22, 1, true);
            contextBuilder = builder.addLine(Component.literal(""), 0xFFFFFFFF, 22, 1, true)
                    .setX(getX()).setY(getY()).setWidth(width).setHeight(height)
                    .build();
        }
        if (blockGoal) {
            int target = goal.active() ? goal.target() : 10_000;
            updateLine(0, Component.literal(tr("evoassist.hud.blocks") + "§b" + goal.blocks + "§f / " + target
                    + (goal.complete ? " §a✓" : "")));
            updateLine(1, Component.literal(tr("evoassist.hud.time") + "" + TimeUtils.asTextTime(goal.activeMillis)));
            updateLine(2, Component.literal(tr("evoassist.hud.mined") + "§a" + MoneyUtils.convertTo(goal.money)
                    + "§f\uE135 / §d" + MoneyUtils.convertTo(goal.shards) + "§f\uE365"));
        } else {
            long targetMillis = (goal.active() ? goal.target() : 60) * 60_000L;
            Component timeLine = Component.literal(tr("evoassist.hud.time") + "" + TimeUtils.asTextTime(goal.activeMillis)
                    + " / " + TimeUtils.asTextTime(targetMillis) + (goal.complete ? " §a✓" : ""));
            updateLine(0, timeLine);
            setBaseWidth(Math.max(230, 24 + EvoAssistClient.instance.font.width(timeLine)));
            updateLine(1, Component.literal(tr("evoassist.hud.mined") + "§b" + MoneyUtils.convertTo(goal.blocks)
                    + "§f\uE127 / §a" + MoneyUtils.convertTo(goal.money)
                    + "§f\uE135 / §d" + MoneyUtils.convertTo(goal.shards) + "§f\uE365"));
        }
        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void savePosition() {
        if (blockGoal) {
            HudConfig.BlockGoalX = getX();
            HudConfig.BlockGoalY = getY();
        } else {
            HudConfig.TimeGoalX = getX();
            HudConfig.TimeGoalY = getY();
        }
    }

    @Override
    protected void saveScale(double scale) {
        if (blockGoal) HudConfig.BlockGoalScale = scale;
        else HudConfig.TimeGoalScale = scale;
    }

    @Override
    protected double loadScale() {
        return blockGoal ? HudConfig.BlockGoalScale : HudConfig.TimeGoalScale;
    }
}
