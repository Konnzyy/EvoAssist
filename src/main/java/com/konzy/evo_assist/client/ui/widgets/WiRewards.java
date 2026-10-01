package com.konzy.evo_assist.client.ui.widgets;

import static com.konzy.evo_assist.client.util.Texts.tr;
import com.konzy.evo_assist.client.Evo_assistClient;
import com.konzy.evo_assist.client.config.ConfigBosses;
import com.konzy.evo_assist.client.config.ConfigClan;
import com.konzy.evo_assist.client.config.Hidden.HudConfig;
import com.konzy.evo_assist.client.features.rewards.RewardMessageParser;
import com.konzy.evo_assist.client.ui.WidgetScreen;
import com.konzy.evo_assist.client.ui.elements.ContextBuilder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class WiRewards extends WWidget {
    private final boolean clan;

    public WiRewards(boolean clan, int x, int y, WidgetScreen editor) {
        super(x, y, 230, 31, editor, clan ? HudConfig.ClanScale : HudConfig.BossScale);
        this.clan = clan;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (widgetScreen == null && (!Evo_assistClient.rewardStatistics.isConnected()
                || !(clan ? ConfigClan.widgetEnabled : ConfigBosses.widgetEnabled))) {
            hide(true);
            return;
        }
        hide(false);
        if (contextBuilder == null) {
            Identifier icon = Identifier.fromNamespaceAndPath("minecraft",
                    clan ? "textures/item/gold_ingot.png" : "textures/item/netherite_sword.png");
            contextBuilder = new ContextBuilder.Builder()
                    .addTexture(icon, 0, 0, true, width, height, 24).setPadding(10)
                    .addLine(Component.empty(), 0xFFFFFFFF, 26, 1, true)
                    .addLine(Component.empty(), 0xFFFFFFFF, 26, 1, true)
                    .addLine(Component.empty(), 0xFFFFFFFF, 26, 1, true)
                    .setX(getX()).setY(getY()).setWidth(width).setHeight(height).build();
        }
        var totals = Evo_assistClient.rewardStatistics.current();
        if (clan) {
            updateLine(0, Component.literal(tr("evoassist.hud.clanPoints") + "§b" + RewardMessageParser.whole(totals.clanPoints)));
            updateLine(1, Component.literal(tr("evoassist.hud.clanExperience") + "§a" + RewardMessageParser.whole(totals.clanExperience)));
            updateLine(2, Component.literal(tr("evoassist.hud.clanGold") + "§e" + RewardMessageParser.whole(totals.clanGold)));
        } else {
            updateLine(0, Component.literal(tr("evoassist.hud.money") + "§a" + RewardMessageParser.compactMoney(totals.money) + "§f\uE135"));
            updateLine(1, Component.literal(tr("evoassist.hud.shards") + "§d" + RewardMessageParser.whole(totals.shards) + "§f\uE365"));
            updateLine(2, Component.literal(tr("evoassist.hud.tokens") + "§e" + RewardMessageParser.whole(totals.tokens)));
        }
        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void savePosition() {
        if (clan) { HudConfig.ClanX = getX(); HudConfig.ClanY = getY(); }
        else { HudConfig.BossX = getX(); HudConfig.BossY = getY(); }
    }

    @Override
    protected void saveScale(double scale) {
        if (clan) HudConfig.ClanScale = scale;
        else HudConfig.BossScale = scale;
    }

    @Override
    protected double loadScale() { return clan ? HudConfig.ClanScale : HudConfig.BossScale; }
}
