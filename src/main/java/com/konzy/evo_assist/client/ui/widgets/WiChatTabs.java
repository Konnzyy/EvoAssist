package com.konzy.evo_assist.client.ui.widgets;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.chat.ChatPrefix;
import com.konzy.evo_assist.client.chat.ChatTabManager;
import com.konzy.evo_assist.client.chat.ChatTabsLayout;
import com.konzy.evo_assist.client.config.ConfigChat;
import com.konzy.evo_assist.client.config.Hidden.HudConfig;
import com.konzy.evo_assist.client.ui.WidgetScreen;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;

public final class WiChatTabs extends WWidget {
    private final Consumer<ChatPrefix> onPrefix;
    private boolean bottomAnchored;
    private ChatTabsLayout layout;
    private double mouseLocalX, mouseLocalY;

    public WiChatTabs(WidgetScreen editor, Consumer<ChatPrefix> onPrefix) {
        super(HudConfig.ChatTabsX, Math.max(0, HudConfig.ChatTabsY), 1, 1, editor, HudConfig.ChatTabsScale);
        this.onPrefix = onPrefix;
        bottomAnchored = HudConfig.ChatTabsY < 0;
        refreshLayout();
        hide(false);
    }

    private void refreshLayout() {
        var client = EvoAssistClient.instance;
        layout = new ChatTabsLayout(ChatTabManager.getInstance().getTabs().stream()
                .map(tab -> client.font.width(tab.name()) + 6).toList(),
                ChatPrefix.getAll().stream().map(prefix -> client.font.width(prefix.getText()) + 6).toList(),
                ConfigChat.chatTabsOrientation == ConfigChat.Orientation.VERTICAL);
        int screenWidth = widgetScreen == null ? client.getWindow().getGuiScaledWidth() : widgetScreen.width;
        int screenHeight = widgetScreen == null ? client.getWindow().getGuiScaledHeight() : widgetScreen.height;
        var placement = layout.place(getX(), bottomAnchored ? -1 : getY(), HudConfig.ChatTabsScale, screenWidth, screenHeight);
        scale = placement.scale();
        setBaseWidth(layout.width());
        setBaseHeight(layout.height());
        width = placement.width();
        height = placement.height();
        setX(placement.x());
        setY(placement.y());
        applyPos();
    }

    @Override
    protected void extractWidgetRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        hide(widgetScreen == null && !ConfigChat.chatTabsToggle);
        if (hidden) return;
        refreshLayout();
        mouseLocalX = (mouseX - getX()) / scale;
        mouseLocalY = (mouseY - getY()) / scale;
        if (widgetScreen != null) {
            graphics.text(EvoAssistClient.instance.font, "x" + Math.round(scale * 10) / 10.0,
                    getX(), getY() - 10, 0xFFFFFFFF, true);
        }
        // Keep the fitted, rounded bounds for both drawing and hit testing.
        graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
        graphics.pose().pushMatrix();
        graphics.pose().translate(-((float) scale - 1) * getX(), -((float) scale - 1) * getY());
        graphics.pose().scale((float) scale, (float) scale);
        renderBg(graphics);
        graphics.pose().popMatrix();
        graphics.disableScissor();
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor graphics) {
        super.renderBg(graphics);
        var manager = ChatTabManager.getInstance();
        for (var cell : layout.cells()) {
            boolean hover = cell.contains(mouseLocalX, mouseLocalY);
            int textColor;
            boolean selected;
            String label;
            if (cell.prefix()) {
                var prefix = ChatPrefix.getAll().get(cell.index());
                selected = manager.getCurrentPrefix() == prefix;
                label = prefix.getText();
                textColor = selected ? 0xAAAAAA : hover ? 0xFFFF55 : 0xFFFFFF;
            } else {
                var tab = manager.getTabs().get(cell.index());
                selected = manager.getActiveTab() == tab;
                label = tab.name();
                textColor = selected ? tab.selectedColor() : manager.isBanned(tab)
                        ? tab.bannedColor() : hover ? tab.hoveredColor() : tab.color();
            }
            int x = getX() + cell.x(), y = getY() + cell.y();
            graphics.fill(x, y, x + cell.width(), y + cell.height(), hover ? 0xB0354550 : 0x80000000);
            int border = selected ? 0xFF5ACAD5 : hover ? 0xFFB8D5DA : 0x805F7580;
            graphics.fill(x, y, x + cell.width(), y + 1, border);
            graphics.fill(x, y + cell.height() - 1, x + cell.width(), y + cell.height(), border);
            graphics.fill(x, y, x + 1, y + cell.height(), border);
            graphics.fill(x + cell.width() - 1, y, x + cell.width(), y + cell.height(), border);
            graphics.centeredText(EvoAssistClient.instance.font, label, x + cell.width() / 2, y + 2, textColor | 0xFF000000);
        }
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (widgetScreen != null) return super.mouseClicked(event, doubleClick);
        if (!ConfigChat.chatTabsToggle || event.button() != 0) return false;
        refreshLayout();
        var cell = layout.at((event.x() - getX()) / scale, (event.y() - getY()) / scale);
        if (cell == null) return false;
        var manager = ChatTabManager.getInstance();
        if (cell.prefix()) {
            var prefix = ChatPrefix.getAll().get(cell.index());
            manager.setPrefix(prefix);
            if (onPrefix != null) onPrefix.accept(prefix);
        } else {
            var tab = manager.getTabs().get(cell.index());
            if (EvoAssistClient.instance.hasShiftDown()) manager.setBannedTab(tab);
            else manager.setActiveTab(tab);
        }
        playDownSound(EvoAssistClient.instance.getSoundManager());
        return true;
    }

    @Override
    protected void onDrag(@NonNull MouseButtonEvent event, double deltaX, double deltaY) {
        if (deltaX != 0 || deltaY != 0) bottomAnchored = false;
        super.onDrag(event, deltaX, deltaY);
        refreshLayout();
    }

    @Override
    public void resize(double amount) {
        saveScale(Math.clamp(scale + Math.signum(amount) * 0.1, 0.3, 5));
        refreshLayout();
    }

    @Override
    public void savePosition() {
        refreshLayout();
        HudConfig.ChatTabsX = getX();
        HudConfig.ChatTabsY = bottomAnchored ? -1 : getY();
    }
    @Override
    protected void saveScale(double scale) { HudConfig.ChatTabsScale = scale; }
    @Override
    protected double loadScale() { return HudConfig.ChatTabsScale; }
}
