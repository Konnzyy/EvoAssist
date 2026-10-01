package com.konzy.evo_assist.client.ui.widgets;

import static com.konzy.evo_assist.client.util.Texts.tr;
import com.konzy.evo_assist.client.Evo_assistClient;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.config.Hidden.HudConfig;
import com.konzy.evo_assist.client.features.mine.blockPH.BlockProfitPerHour;
import com.konzy.evo_assist.client.ui.WidgetScreen;
import com.konzy.evo_assist.client.ui.elements.ContextBuilder;
import com.konzy.evo_assist.client.util.MoneyUtils;
import com.konzy.evo_assist.client.util.TimeUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class WiBlockProfitPH extends WWidget {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/item/netherite_pickaxe.png");
    private static final int TEXTURE_SIZE = 28;
    private static final int PADDING = 10;

    private BlockProfitPerHour blockProfitPH;
    private long lastUpdateTime = 0;
    private static final long UPDATE_COOLDOWN = 50; //50мс


    public WiBlockProfitPH(int x, int y, int width, int height, WidgetScreen widgetScreen) {
        super(x, y, width, height, widgetScreen, HudConfig.WidgetBphScale);
        this.blockProfitPH = BlockProfitPerHour.getInstance();
        if(this.blockProfitPH == null) {
            this.blockProfitPH = Evo_assistClient.eventBlockProfitPerHour;
        }
    }

    private void ensureContext() { //хуярит контекст когда уже можно, проверка можно сказать..
        if(contextBuilder == null && blockProfitPH != null) {
            contextBuilder = new ContextBuilder.Builder()
                    .addTexture(TEXTURE, 0, 0, true, width, height, TEXTURE_SIZE)
                    .setPadding(PADDING)
                    .addLine(Component.literal(tr("evoassist.hud.time") + TimeUtils.asTextTime(0)), 0xFFFFFFFF, TEXTURE_SIZE+1, 1, true)
                    .addLine(Component.literal(tr("evoassist.hud.perHour") + "§b0§f\uE127 §f/ §a0§f\uE135 §f/ §d0§f\uE365"), 0xFFFFFFFF, TEXTURE_SIZE+1, 1, true)
                    .addLine(Component.literal(tr("evoassist.hud.mined") + "§b0§f\uE127 §f/ §a0§f\uE135 §f/ §d0§f\uE365"), 0xFFFFFFFF, TEXTURE_SIZE+1, 1, true)
                    .setX(getX())
                    .setY(getY())
                    .setScale(1)
                    .setWidth(width)
                    .setHeight(height)
                    .build();
        }
    }
    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if(widgetScreen != null || ConfigMining.bphWidgetToggle) {
            hide(false);
        } else {
            hide(true);
            return;
        }

        ensureContext();
        if(contextBuilder == null) {
            renderBg(context);
            return;
        }

        update();

        super.extractWidgetRenderState(context, mouseX, mouseY, delta);
    }

    private void update() {
        if(blockProfitPH == null || contextBuilder == null) return;

        long now = System.currentTimeMillis();
        if(now - lastUpdateTime > UPDATE_COOLDOWN) {

            updateLine(0, Component.literal(String.format(tr("evoassist.hud.time") + "%s",
                    TimeUtils.asTextTime(blockProfitPH.uptime))));

            updateLine(1, Component.literal(String.format(tr("evoassist.hud.perHour") + "§b%s§f\uE127 §f/ §a%s§f\uE135 §f/ §d%s§f\uE365",
                    MoneyUtils.convertTo(blockProfitPH.BlocksPerHour),
                    MoneyUtils.convertTo(blockProfitPH.MoneyPerHour),
                    MoneyUtils.convertTo(blockProfitPH.ShardsPerHour))));

            updateLine(2, Component.literal(String.format(tr("evoassist.hud.mined") + "§b%s§f\uE127 §f/ §a%s§f\uE135 §f/ §d%s§f\uE365",
                    MoneyUtils.convertTo(blockProfitPH.totalBrokenBlocks),
                    MoneyUtils.convertTo(blockProfitPH.totalMoney),
                    MoneyUtils.convertTo(blockProfitPH.totalShards))));

            lastUpdateTime = now;
        }
    }

    @Override
    protected void savePosition() {
        HudConfig.WidgetBphX = getX();
        HudConfig.WidgetBphY = getY();
    }

    @Override
    protected void saveScale(double scale) {
        HudConfig.WidgetBphScale = scale;
    }

    @Override
    protected double loadScale() {
        return HudConfig.WidgetBphScale;
    }
}
