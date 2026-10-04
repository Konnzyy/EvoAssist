package com.konzy.evo_assist.client.ui.widgets;

import static com.konzy.evo_assist.client.util.Texts.tr;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.ConfigBosses;
import com.konzy.evo_assist.client.config.Hidden.HudConfig;
import com.konzy.evo_assist.client.features.rewards.RewardMessageParser;
import com.konzy.evo_assist.client.ui.WidgetScreen;
import com.konzy.evo_assist.client.ui.elements.ContextBuilder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public final class WiBossTokens extends WWidget {
    public WiBossTokens(int x, int y, WidgetScreen editor) {
        super(x, y, 230, 31, editor, HudConfig.BossTokensScale);
    }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (widgetScreen == null && (!EvoAssistClient.rewardStatistics.isConnected() || !ConfigBosses.tokensWidgetEnabled)) {
            hide(true);
            return;
        }
        hide(false);
        if (contextBuilder == null) {
            Identifier icon = Identifier.fromNamespaceAndPath("minecraft", "textures/item/nether_star.png");
            contextBuilder = new ContextBuilder.Builder()
                    .addTexture(icon, 0, 0, true, width, height, 24).setPadding(10)
                    .addLine(Component.empty(), 0xFFFFFFFF, 26, 1, true)
                    .addLine(Component.empty(), 0xFFFFFFFF, 26, 1, true)
                    .addLine(Component.empty(), 0xFFFFFFFF, 26, 1, true)
                    .setX(getX()).setY(getY()).setWidth(width).setHeight(height).build();
        }
        var totals = EvoAssistClient.rewardStatistics.current();
        updateLine(0, Component.literal(tr("evoassist.hud.ordinaryTokens") + "§e" + RewardMessageParser.whole(totals.tokens)));
        updateLine(1, Component.literal(tr("evoassist.hud.infernalTokens") + "§c" + RewardMessageParser.whole(totals.infernalTokens)));
        updateLine(2, Component.literal(tr("evoassist.hud.endTokens") + "§d" + RewardMessageParser.whole(totals.endTokens)));
        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void savePosition() {
        HudConfig.BossTokensX = getX();
        HudConfig.BossTokensY = getY();
    }

    @Override
    protected void saveScale(double scale) { HudConfig.BossTokensScale = scale; }

    @Override
    protected double loadScale() { return HudConfig.BossTokensScale; }
}
