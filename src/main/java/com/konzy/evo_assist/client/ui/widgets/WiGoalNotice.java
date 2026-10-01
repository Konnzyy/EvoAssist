package com.konzy.evo_assist.client.ui.widgets;

import com.konzy.evo_assist.client.config.Hidden.HudConfig;
import com.konzy.evo_assist.client.Evo_assistClient;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import com.konzy.evo_assist.client.ui.WidgetScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class WiGoalNotice extends WWidget {
    private static final int DEFAULT_WIDTH = 260;
    private static final int NOTICE_HEIGHT = 18;
    private boolean centered;
    private int noticeWidth = DEFAULT_WIDTH;
    private Component message = Component.empty();

    public WiGoalNotice(int x, int y, WidgetScreen editor) {
        super(Math.max(0, x), Math.max(0, y), DEFAULT_WIDTH, NOTICE_HEIGHT, editor, HudConfig.GoalNoticeScale);
        centered = x < 0 && y < 0;
        centerIfNeeded();
    }

    private void centerIfNeeded() {
        boolean autoCenter = widgetScreen == null
                ? HudConfig.GoalNoticeX < 0 && HudConfig.GoalNoticeY < 0 : centered;
        if (!autoCenter) return;
        int screenWidth = widgetScreen == null
                ? Evo_assistClient.instance.getWindow().getGuiScaledWidth() : widgetScreen.width;
        int screenHeight = widgetScreen == null
                ? Evo_assistClient.instance.getWindow().getGuiScaledHeight() : widgetScreen.height;
        setX(Math.max(0, (int) Math.round((screenWidth - noticeWidth * scale) / 2)));
        setY(Math.max(0, (int) Math.round((screenHeight - NOTICE_HEIGHT * scale) / 2)));
        applyPos();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        String notice = MiningGoals.getInstance().currentNotice();
        if (notice == null && widgetScreen == null) {
            hide(true);
            return;
        }
        hide(false);
        message = Component.literal(notice == null ? MiningGoals.getInstance().previewNotice() : notice);
        noticeWidth = Math.max(DEFAULT_WIDTH, Evo_assistClient.instance.font.width(message) + 16);
        setBaseWidth(noticeWidth);
        centerIfNeeded();
        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor graphics) {
        super.renderBg(graphics);
        graphics.centeredText(Evo_assistClient.instance.font, message,
                getX() + noticeWidth / 2, getY() + 5, 0xFF9DF2B1);
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        if (deltaX != 0 || deltaY != 0) centered = false;
        super.onDrag(event, deltaX, deltaY);
    }

    @Override
    public void savePosition() {
        HudConfig.GoalNoticeX = centered ? -1 : getX();
        HudConfig.GoalNoticeY = centered ? -1 : getY();
    }

    @Override
    protected void saveScale(double scale) { HudConfig.GoalNoticeScale = scale; }

    @Override
    protected double loadScale() { return HudConfig.GoalNoticeScale; }
}
