package com.konzy.evo_assist.client.ui.widgets;

import static com.konzy.evo_assist.client.util.Texts.tr;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.ConfigClan;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.config.Hidden.HudConfig;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals.Type;
import com.konzy.evo_assist.client.ui.WidgetScreen;
import com.konzy.evo_assist.client.ui.elements.ContextBuilder;
import com.konzy.evo_assist.client.util.GoalAmount;
import com.konzy.evo_assist.client.util.TimeUtils;
import java.math.BigDecimal;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public final class WiAdditionalGoal extends WWidget {
    private final Type type;
    public WiAdditionalGoal(Type type, WidgetScreen editor) {
        super(positionX(type), positionY(type), 230, type.clan ? 13 : 31, editor, configuredScale(type));
        this.type = type;
    }

    public boolean enabled() {
        return switch (type) {
            case MONEY -> ConfigMining.moneyGoalWidgetEnabled;
            case SHARDS -> ConfigMining.shardGoalWidgetEnabled;
            case CLAN_POINTS -> ConfigClan.pointsGoalWidgetEnabled;
            case CLAN_GOLD -> ConfigClan.goldGoalWidgetEnabled;
            case CLAN_EXPERIENCE -> ConfigClan.experienceGoalWidgetEnabled;
        };
    }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        var goals = AdditionalGoals.getInstance();
        var goal = goals.goal(type);
        if (widgetScreen == null && (!goal.active() || !enabled() || type.clan && !goals.connected())) {
            hide(true);
            return;
        }
        hide(false);
        if (contextBuilder == null) {
            var builder = new ContextBuilder.Builder().setPadding(10);
            String texture = switch (type) {
                case MONEY, SHARDS -> "item/netherite_pickaxe";
                case CLAN_POINTS -> "block/sunflower_front";
                case CLAN_GOLD -> "item/gold_ingot";
                case CLAN_EXPERIENCE -> "item/experience_bottle";
            };
            builder.addTexture(Identifier.fromNamespaceAndPath("minecraft", "textures/" + texture + ".png"),
                    type.clan ? 1 : 0, 0, true, 230, type.clan ? 13 : 31, type.clan ? 12 : 20);
            builder.addLine(Component.empty(), 0xFFFFFFFF, type.clan ? 16 : 22, type.clan ? 2 : 1, true);
            if (!type.clan) builder.addLine(Component.empty(), 0xFFFFFFFF, 22, 1, true)
                    .addLine(Component.empty(), 0xFFFFFFFF, 22, 1, true);
            contextBuilder = builder.setX(getX()).setY(getY()).setWidth(230).setHeight(type.clan ? 13 : 31).build();
        }
        String color = switch (type) {
            case MONEY, CLAN_EXPERIENCE -> "§a";
            case SHARDS -> "§d";
            case CLAN_POINTS -> "§b";
            case CLAN_GOLD -> "§e";
        };
        BigDecimal preview = switch (type) {
            case MONEY -> BigDecimal.valueOf(5_000_000_000L);
            case SHARDS -> BigDecimal.valueOf(1000);
            case CLAN_POINTS -> BigDecimal.valueOf(5000);
            case CLAN_GOLD -> BigDecimal.valueOf(100);
            case CLAN_EXPERIENCE -> BigDecimal.valueOf(50);
        };
        String target = AdditionalGoals.format(type, goal.active() ? goal.target : preview);
        String value = AdditionalGoals.format(type, goal.progress);
        String icon = type == Type.MONEY ? "\uE135" : type == Type.SHARDS ? "\uE365" : "";
        Component progressLine = Component.literal((type.clan ? tr(type.title) : tr("evoassist.hud.received")) + ": " + color + value
                + "§f" + icon + " / " + color + target + "§f" + icon + (goal.complete ? " §a✓" : ""));
        updateLine(0, progressLine);
        setBaseWidth(Math.max(230, (type.clan ? 20 : 26) + EvoAssistClient.instance.font.width(progressLine)));
        if (!type.clan) {
            updateLine(1, Component.literal(tr("evoassist.hud.time") + "" + TimeUtils.asTextTime(goal.activeMillis)));
            String secondary = type == Type.MONEY
                    ? "§d" + GoalAmount.format(BigDecimal.valueOf(goal.shards)) + "§f\uE365"
                    : "§a" + GoalAmount.format(goal.money) + "§f\uE135";
            updateLine(2, Component.literal(tr("evoassist.hud.mined") + "§b" + GoalAmount.format(BigDecimal.valueOf(goal.blocks))
                    + "§f\uE127 / " + secondary));
        }
        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
    }

    public static int positionX(Type type) {
        return switch (type) {
            case MONEY -> HudConfig.MoneyGoalX;
            case SHARDS -> HudConfig.ShardGoalX;
            case CLAN_POINTS -> HudConfig.ClanPointsGoalX;
            case CLAN_GOLD -> HudConfig.ClanGoldGoalX;
            case CLAN_EXPERIENCE -> HudConfig.ClanExperienceGoalX;
        };
    }
    public static int positionY(Type type) {
        return switch (type) {
            case MONEY -> HudConfig.MoneyGoalY;
            case SHARDS -> HudConfig.ShardGoalY;
            case CLAN_POINTS -> HudConfig.ClanPointsGoalY;
            case CLAN_GOLD -> HudConfig.ClanGoldGoalY;
            case CLAN_EXPERIENCE -> HudConfig.ClanExperienceGoalY;
        };
    }
    public static double configuredScale(Type type) {
        return switch (type) {
            case MONEY -> HudConfig.MoneyGoalScale;
            case SHARDS -> HudConfig.ShardGoalScale;
            case CLAN_POINTS -> HudConfig.ClanPointsGoalScale;
            case CLAN_GOLD -> HudConfig.ClanGoldGoalScale;
            case CLAN_EXPERIENCE -> HudConfig.ClanExperienceGoalScale;
        };
    }
    @Override
    public void savePosition() {
        switch (type) {
            case MONEY -> { HudConfig.MoneyGoalX = getX(); HudConfig.MoneyGoalY = getY(); }
            case SHARDS -> { HudConfig.ShardGoalX = getX(); HudConfig.ShardGoalY = getY(); }
            case CLAN_POINTS -> { HudConfig.ClanPointsGoalX = getX(); HudConfig.ClanPointsGoalY = getY(); }
            case CLAN_GOLD -> { HudConfig.ClanGoldGoalX = getX(); HudConfig.ClanGoldGoalY = getY(); }
            case CLAN_EXPERIENCE -> { HudConfig.ClanExperienceGoalX = getX(); HudConfig.ClanExperienceGoalY = getY(); }
        }
    }
    @Override
    protected void saveScale(double scale) {
        switch (type) {
            case MONEY -> HudConfig.MoneyGoalScale = scale;
            case SHARDS -> HudConfig.ShardGoalScale = scale;
            case CLAN_POINTS -> HudConfig.ClanPointsGoalScale = scale;
            case CLAN_GOLD -> HudConfig.ClanGoldGoalScale = scale;
            case CLAN_EXPERIENCE -> HudConfig.ClanExperienceGoalScale = scale;
        }
    }
    @Override
    protected double loadScale() { return configuredScale(type); }
}
