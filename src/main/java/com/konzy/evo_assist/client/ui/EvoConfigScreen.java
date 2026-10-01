package com.konzy.evo_assist.client.ui;

import static com.konzy.evo_assist.client.util.Texts.tr;
import com.konzy.evo_assist.client.Evo_assistClient;
import com.konzy.evo_assist.client.config.Config;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import com.konzy.evo_assist.client.util.GoalAmount;
import com.konzy.evo_assist.client.config.ConfigAutoclicker;
import com.konzy.evo_assist.client.config.ConfigChat;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.config.ConfigVisual;
import com.konzy.evo_assist.client.config.ConfigBosses;
import com.konzy.evo_assist.client.config.ConfigClan;
import com.konzy.evo_assist.client.features.autoclicker.Clicker;
import com.konzy.evo_assist.client.features.mine.blockPH.BlockProfitPerHour;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvoConfigScreen extends Screen {
    private static final int BACKDROP = 0x421A2028;
    private static final int PANEL = 0xDF35404B;
    private static final int SIDEBAR = 0xE32C3641;
    private static final int CARD = 0xE3545F6B;
    private static final int CARD_HOVER = 0xEE626E7B;
    private static final int CONTROL = 0xD933414F;
    private static final int ACCENT = 0xFF43C8F3;
    private static final int TEXT = 0xFFF5F7F9;
    private static final int MUTED = 0xFFD1DAE2;
    private static final int TRACK = 0xFF71808D;

    private final Screen parent;
    private final String modVersion;
    private Page page = Page.AUTOCLICKER;
    private int scroll;
    private Row draggingSlider;
    private String openDropdown;
    private String capturingKeyId;
    private String editingId;
    private String editingText = "";
    private String feedback;
    private long feedbackUntil;
    private String resetFlashId;
    private long resetFlashAt;
    private String pendingResetId;
    private static final long RESET_FLASH_DURATION_MS = 650;
    private final Map<String, Float> hoverProgress = new HashMap<>();
    private long lastHoverFrameNanos;
    private float hoverStep = 1.0f;

    private enum Page {
        AUTOCLICKER("evoassist.page.autoclicker.title", "evoassist.page.autoclicker.subtitle"),
        BOSSES("evoassist.page.bosses.title", "evoassist.page.bosses.subtitle"),
        MINING("evoassist.page.mining.title", "evoassist.page.mining.subtitle"),
        MINING_GOALS("evoassist.page.mining_goals.title", "evoassist.page.mining_goals.subtitle"),
        CLAN("evoassist.page.clan.title", "evoassist.page.clan.subtitle"),
        CLAN_GOALS("evoassist.page.clan_goals.title", "evoassist.page.clan_goals.subtitle"),
        INTERFACE("evoassist.page.interface.title", "evoassist.page.interface.subtitle");

        final String title;
        final String subtitle;

        Page(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private enum Kind { TOGGLE, CHOICE, MULTI, SLIDER, CPS, NUMBER, AMOUNT, KEY, ACTION, EDITOR, SECTION }

    private record DropdownOption(String id, String title) {}
    private record DropdownBounds(int x, int y, int width, int height) {}

    private record Row(String id, String title, String hint, Kind kind, int min, int max) {
        static Row section(String title) {
            return new Row("", title, "", Kind.SECTION, 0, 0);
        }
        static Row toggle(String id, String title, String hint) {
            return new Row(id, title, hint, Kind.TOGGLE, 0, 0);
        }
        static Row choice(String id, String title, String hint) {
            return new Row(id, title, hint, Kind.CHOICE, 0, 0);
        }
        static Row slider(String id, String title, String hint, int min, int max) {
            return new Row(id, title, hint, Kind.SLIDER, min, max);
        }
        static Row number(String id, String title, String hint, int min, int max) {
            return new Row(id, title, hint, Kind.NUMBER, min, max);
        }
        static Row amount(String id, String title, String hint) {
            return new Row(id, title, hint, Kind.AMOUNT, 0, 0);
        }
        static Row action(String id, String title, String hint) {
            return new Row(id, title, hint, Kind.ACTION, 0, 0);
        }
        static Row editor(String id, String title, String hint) {
            return new Row(id, title, hint, Kind.EDITOR, 0, 0);
        }
    }

    public EvoConfigScreen(Screen parent) {
        super(Component.literal("EvoAssist"));
        this.parent = parent;
        String fullVersion = FabricLoader.getInstance().getModContainer(Evo_assistClient.MODID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        this.modVersion = fullVersion.split("\\+")[0];
        ConfigVisual.menuOpacity = Math.max(10, Math.min(100, ConfigVisual.menuOpacity));
        ConfigVisual.goalNoticeDurationSeconds = Math.max(1, Math.min(60, ConfigVisual.goalNoticeDurationSeconds));
    }

    private List<Row> rows() {
        List<Row> items = new ArrayList<>();
        switch (page) {
            case AUTOCLICKER -> {
                items.add(Row.toggle("clickerEnabled", tr("evoassist.ui.clickerEnabled.title"), tr("evoassist.ui.clickerEnabled.hint")));
                items.add(Row.choice("clickerButton", tr("evoassist.ui.clickerButton.title"), tr("evoassist.ui.clickerButton.hint")));
                items.add(Row.choice("clickerMode", tr("evoassist.ui.clickerMode.title"), tr("evoassist.ui.clickerMode.hint")));
                items.add(new Row("clickerKey", tr("evoassist.ui.clickerKey.title"), tr("evoassist.ui.key.hint"), Kind.KEY, 0, 0));
                items.add(new Row("clickerModeKey", tr("evoassist.ui.clickerModeKey.title"), tr("evoassist.ui.key.hint"), Kind.KEY, 0, 0));
                items.add(new Row("cps", tr("evoassist.ui.cps.title"), tr("evoassist.ui.cps.hint"), Kind.CPS, 1, 20));
            }
            case MINING -> {
                items.add(Row.toggle("miningWidget", tr("evoassist.ui.miningWidget.title"), tr("evoassist.ui.miningWidget.hint")));
                items.add(new Row("allowed", tr("evoassist.ui.allowed.title"), tr("evoassist.ui.allowed.hint"), Kind.MULTI, 0, 0));
                items.add(Row.action("resetMining", tr("evoassist.ui.resetMining.title"), tr("evoassist.ui.resetMining.hint")));
                items.add(Row.editor("editMining", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
            }
            case MINING_GOALS -> {
                items.add(Row.section(tr("evoassist.section.blocks")));
                items.add(Row.toggle("blockGoalWidget", tr("evoassist.ui.blockGoalWidget.title"), tr("evoassist.ui.blockGoalWidget.hint")));
                items.add(Row.number("blockGoal", tr("evoassist.ui.blockGoal.title"), tr("evoassist.ui.blockGoal.hint"), 0, 9_999_999));
                items.add(Row.editor("editBlockGoal", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
                items.add(Row.section(tr("evoassist.section.time")));
                items.add(Row.toggle("timeGoalWidget", tr("evoassist.ui.timeGoalWidget.title"), tr("evoassist.ui.timeGoalWidget.hint")));
                items.add(Row.number("timeGoal", tr("evoassist.ui.timeGoal.title"), tr("evoassist.ui.timeGoal.hint"), 0, 9_999));
                items.add(Row.editor("editTimeGoal", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
                items.add(Row.section(tr("evoassist.section.money")));
                items.add(Row.toggle("moneyGoalWidget", tr("evoassist.ui.moneyGoalWidget.title"), tr("evoassist.ui.moneyGoalWidget.hint")));
                items.add(Row.amount("moneyGoal", tr("evoassist.ui.moneyGoal.title"), tr("evoassist.ui.moneyGoal.hint")));
                items.add(Row.editor("editMoneyGoal", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
                items.add(Row.section(tr("evoassist.section.shards")));
                items.add(Row.toggle("shardGoalWidget", tr("evoassist.ui.shardGoalWidget.title"), tr("evoassist.ui.shardGoalWidget.hint")));
                items.add(Row.amount("shardGoal", tr("evoassist.ui.shardGoal.title"), tr("evoassist.ui.shardGoal.hint")));
                items.add(Row.editor("editShardGoal", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
                items.add(Row.section(tr("evoassist.section.general")));
                items.add(Row.toggle("goalNotifications", tr("evoassist.ui.goalNotifications.title"), tr("evoassist.ui.goalNotifications.hint")));
                items.add(Row.slider("goalNoticeDuration", tr("evoassist.ui.goalNoticeDuration.title"), tr("evoassist.ui.goalNoticeDuration.hint"), 1, 60));
                items.add(Row.editor("editGoalNotice", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
                items.add(Row.action("resetGoals", tr("evoassist.ui.resetGoals.title"), tr("evoassist.ui.resetGoals.hint")));
            }
            case BOSSES -> {
                items.add(Row.toggle("bossWidget", tr("evoassist.ui.bossWidget.title"), tr("evoassist.ui.bossWidget.hint")));
                items.add(Row.action("resetBosses", tr("evoassist.ui.resetRewards.title"), tr("evoassist.ui.resetBosses.hint")));
                items.add(Row.editor("editBosses", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
            }
            case CLAN -> {
                items.add(Row.toggle("clanWidget", tr("evoassist.ui.clanWidget.title"), tr("evoassist.ui.clanWidget.hint")));
                items.add(Row.action("resetClan", tr("evoassist.ui.resetRewards.title"), tr("evoassist.ui.resetClan.hint")));
                items.add(Row.editor("editClan", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
            }
            case CLAN_GOALS -> {
                items.add(Row.section(tr("evoassist.section.clanPoints")));
                items.add(Row.toggle("clanPointsGoalWidget", tr("evoassist.ui.clanPointsGoalWidget.title"), tr("evoassist.ui.clanPointsGoalWidget.hint")));
                items.add(Row.amount("clanPointsGoal", tr("evoassist.ui.clanPointsGoal.title"), tr("evoassist.ui.clanPointsGoal.hint")));
                items.add(Row.editor("editClanPointsGoal", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
                items.add(Row.section(tr("evoassist.section.clanGold")));
                items.add(Row.toggle("clanGoldGoalWidget", tr("evoassist.ui.clanGoldGoalWidget.title"), tr("evoassist.ui.clanGoldGoalWidget.hint")));
                items.add(Row.amount("clanGoldGoal", tr("evoassist.ui.clanGoldGoal.title"), tr("evoassist.ui.clanGoldGoal.hint")));
                items.add(Row.editor("editClanGoldGoal", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
                items.add(Row.section(tr("evoassist.section.clanExperience")));
                items.add(Row.toggle("clanExperienceGoalWidget", tr("evoassist.ui.clanExperienceGoalWidget.title"), tr("evoassist.ui.clanExperienceGoalWidget.hint")));
                items.add(Row.amount("clanExperienceGoal", tr("evoassist.ui.clanExperienceGoal.title"), tr("evoassist.ui.clanExperienceGoal.hint")));
                items.add(Row.editor("editClanExperienceGoal", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
                items.add(Row.section(tr("evoassist.section.general")));
                items.add(Row.toggle("clanGoalNotifications", tr("evoassist.ui.goalNotifications.title"), tr("evoassist.ui.clanGoalNotifications.hint")));
                items.add(Row.slider("goalNoticeDuration", tr("evoassist.ui.goalNoticeDuration.title"), tr("evoassist.ui.goalNoticeDuration.hint"), 1, 60));
                items.add(Row.editor("editClanGoalNotice", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
                items.add(Row.action("resetClanGoals", tr("evoassist.ui.resetGoals.title"), tr("evoassist.ui.resetClanGoals.hint")));
            }
            case INTERFACE -> {
                items.add(Row.section(tr("evoassist.section.chat")));
                items.add(Row.toggle("chatTabs", tr("evoassist.ui.chatTabs.title"), tr("evoassist.ui.chatTabs.hint")));
                items.add(Row.choice("chatOrientation", tr("evoassist.ui.chatOrientation.title"), tr("evoassist.ui.chatOrientation.hint")));
                items.add(Row.editor("editChatTabs", tr("evoassist.ui.editor.title"), tr("evoassist.ui.editor.hint")));
                items.add(Row.section(tr("evoassist.section.menu")));
                items.add(new Row("menuKey", tr("evoassist.ui.menuKey.title"), tr("evoassist.ui.key.hint"), Kind.KEY, 0, 0));
                items.add(Row.slider("menuOpacity", tr("evoassist.ui.menuOpacity.title"), tr("evoassist.ui.menuOpacity.hint"), 10, 100));
            }
        }
        return items;
    }

    private int panelWidth() { return Math.min(width - 16, Math.min(900, Math.round(width * 0.84f))); }
    private int panelHeight() { return Math.min(height - 16, Math.min(550, Math.round(height * 0.86f))); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    private int right() { return left() + panelWidth(); }
    private int bottom() { return top() + panelHeight(); }
    private int sidebarWidth() { return Math.min(148, Math.max(94, panelWidth() / 4)); }
    private int contentX() { return left() + sidebarWidth() + 8; }
    private int contentWidth() { return right() - contentX(); }
    private int contentTop() { return top() + 48; }
    private int contentBottom() { return bottom() - 38; }
    private int navStep() { return Math.min(29, Math.max(12, (panelHeight() - 56) / Page.values().length)); }
    private int navHeight() { return Math.min(24, navStep() - 2); }
    private int rowHeight(Row row) {
        if (row.kind == Kind.SECTION) return 24;
        return contentWidth() < 340 ? 69 : 56;
    }
    private int rowStep(Row row) { return rowHeight(row) + 7; }

    private int maxScroll() {
        int total = 0;
        for (Row row : rows()) total += rowStep(row);
        return Math.max(0, total - (contentBottom() - contentTop()));
    }

    private int menuColor(int color) {
        int opacity = Math.max(10, Math.min(100, ConfigVisual.menuOpacity));
        int alpha = (color >>> 24) * opacity / 100;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private void surface(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, menuColor(color));
    }

    private float hoverAmount(String id, boolean hovered) {
        float previous = hoverProgress.getOrDefault(id, 0.0f);
        float target = hovered ? 1.0f : 0.0f;
        float current = previous + (target - previous) * hoverStep;
        hoverProgress.put(id, current);
        return current;
    }

    private static int mixColor(int from, int to, float amount) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int start = (from >>> shift) & 0xFF;
            int end = (to >>> shift) & 0xFF;
            result |= (Math.round(start + (end - start) * amount) & 0xFF) << shift;
        }
        return result;
    }

    private void renderControl(GuiGraphicsExtractor g, String id, int x, int y, int w, int h,
                               int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, h)
                && (id.equals("done") || (mouseY >= contentTop() && mouseY < contentBottom()));
        float amount = hoverAmount(id, hovered);
        surface(g, x, y, x + w, y + h, mixColor(CONTROL, 0xF05B7183, amount));
        int border = mixColor(0xFF8594A1, ACCENT, amount);
        surface(g, x, y, x + w, y + 1, border);
        surface(g, x, y + h - 1, x + w, y + h, border);
        surface(g, x, y, x + 1, y + h, border);
        surface(g, x + w - 1, y, x + w, y + h, border);
    }

    private String fit(String text, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        while (!text.isEmpty() && font.width(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // This screen draws its own adjustable translucent backdrop.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        if (lastHoverFrameNanos != 0) {
            hoverStep = Math.min(1.0f, Math.max(0.0f, (now - lastHoverFrameNanos) / 100_000_000.0f));
        }
        lastHoverFrameNanos = now;
        scroll = Math.min(scroll, maxScroll());
        surface(g, 0, 0, width, height, BACKDROP);
        surface(g, left(), top(), right(), bottom(), PANEL);
        surface(g, left(), top(), left() + sidebarWidth(), bottom(), SIDEBAR);
        surface(g, left() + sidebarWidth(), top(), left() + sidebarWidth() + 1, bottom(), 0xFF83919E);

        g.text(font, "EvoAssist", left() + 12, top() + 11, TEXT);
        g.text(font, modVersion, left() + 12, top() + 26, MUTED);
        renderWidgetEditorButton(g, mouseX, mouseY);

        int navY = top() + 48;
        for (Page item : Page.values()) {
            boolean active = item == page;
            int navX = left() + 7;
            int navW = sidebarWidth() - 14;
            boolean hover = inside(mouseX, mouseY, navX, navY, navW, navHeight());
            float amount = hoverAmount("nav:" + item.name(), hover);
            if (active || amount > 0.01f) surface(g, navX, navY, navX + navW,
                    navY + navHeight(), active ? 0xFF547087 : mixColor(SIDEBAR, CARD_HOVER, amount));
            int navBorder = mixColor(0xFF7D8B98, ACCENT, active ? 1.0f : amount);
            surface(g, navX, navY, navX + navW, navY + 1, navBorder);
            surface(g, navX, navY + navHeight() - 1, navX + navW, navY + navHeight(), navBorder);
            surface(g, navX, navY, navX + 1, navY + navHeight(), navBorder);
            surface(g, navX + navW - 1, navY, navX + navW, navY + navHeight(), navBorder);
            if (active) surface(g, navX, navY, navX + 3, navY + navHeight(), ACCENT);
            g.text(font, fit(tr(item.title), sidebarWidth() - 28), left() + 16,
                    navY + Math.max(4, (navHeight() - 8) / 2), active ? ACCENT : TEXT);
            navY += navStep();
        }

        g.text(font, tr(page.title), contentX() + 10, top() + 10, TEXT);
        g.text(font, fit(tr(page.subtitle), contentWidth() - 20), contentX() + 10, top() + 26, MUTED);
        surface(g, contentX() + 8, top() + 42, right() - 8, top() + 43, 0xFF81909E);

        g.enableScissor(contentX(), contentTop(), right(), contentBottom());
        int y = contentTop() - scroll;
        for (Row row : rows()) {
            int rowH = rowHeight(row);
            if (y + rowH >= contentTop() && y < contentBottom()) {
                renderRow(g, row, y, rowH, mouseX, mouseY);
            }
            y += rowStep(row);
        }
        g.disableScissor();

        if (maxScroll() > 0) {
            int trackHeight = contentBottom() - contentTop();
            int thumbHeight = Math.max(16, trackHeight * trackHeight / (trackHeight + maxScroll()));
            int thumbY = contentTop() + (trackHeight - thumbHeight) * scroll / maxScroll();
            surface(g, right() - 5, contentTop(), right() - 3, contentBottom(), TRACK);
            surface(g, right() - 6, thumbY, right() - 2, thumbY + thumbHeight, ACCENT);
        }

        surface(g, contentX(), contentBottom() + 3, right(), bottom(), PANEL);
        String footer = feedback != null && System.currentTimeMillis() < feedbackUntil
                ? feedback : tr("evoassist.ui.saveHint");
        g.text(font, fit(footer, contentWidth() - 90), contentX() + 10, bottom() - 22, MUTED);
        renderControl(g, "done", right() - 69, bottom() - 31, 60, 24, mouseX, mouseY);
        g.centeredText(font, tr("evoassist.ui.done"), right() - 39, bottom() - 23, TEXT);
        renderDropdown(g, mouseX, mouseY);
        if (pendingResetId != null) renderResetConfirmation(g, mouseX, mouseY);
    }

    private int cardX() { return contentX() + 8; }
    private int cardWidth() { return contentWidth() - 16; }
    private boolean narrow() { return contentWidth() < 340; }
    private int controlWidth() { return narrow() ? cardWidth() - 24 : Math.min(192, Math.max(155, cardWidth() / 3)); }
    private int controlX() { return narrow() ? cardX() + 12 : cardX() + cardWidth() - controlWidth() - 12; }
    private int controlY(int rowY) { return narrow() ? rowY + 37 : rowY + 16; }
    private int resetX() { return controlX() + controlWidth() - 39; }
    private int valueX() { return resetX() - 44; }
    private int sliderStart(Row row) { return controlX() + (row.kind == Kind.CPS ? font.width("1") + 10 : 0); }
    private int sliderWidth(Row row) {
        int rightEdge = valueX() - 8;
        if (row.kind == Kind.CPS) rightEdge -= font.width("20") + 10;
        return Math.max(20, rightEdge - sliderStart(row));
    }
    private int comboWidth() { return Math.min(130, controlWidth()); }
    private int comboX() { return controlX() + controlWidth() - comboWidth(); }
    private int widgetEditorButtonX() { return left() + sidebarWidth() - 28; }
    private int widgetEditorButtonY() { return top() + 8; }

    private void renderWidgetEditorButton(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = widgetEditorButtonX();
        int y = widgetEditorButtonY();
        boolean hover = inside(mouseX, mouseY, x, y, 20, 20);
        float amount = hoverAmount("widgetEditor", hover);
        surface(g, x, y, x + 20, y + 20, mixColor(0xFF82909B, ACCENT, amount));
        surface(g, x + 1, y + 1, x + 19, y + 19, mixColor(CONTROL, CARD_HOVER, amount));
        surface(g, x + 9, y + 4, x + 11, y + 16, TEXT);
        surface(g, x + 4, y + 9, x + 16, y + 11, TEXT);
        if (hover) g.text(font, fit(tr("evoassist.ui.widgetSettings"), sidebarWidth() - 16), left() + 8, top() + 37, TEXT);
    }

    private List<DropdownOption> dropdownOptions() {
        if (openDropdown == null) return List.of();
        return switch (openDropdown) {
            case "clickerButton" -> List.of(new DropdownOption("buttonLeft", tr("evoassist.ui.leftMouse")),
                    new DropdownOption("buttonRight", tr("evoassist.ui.rightMouse")));
            case "clickerMode" -> List.of(new DropdownOption("modeClick", tr("evoassist.ui.clickMode")),
                    new DropdownOption("modeHold", tr("evoassist.ui.holdMode")));
            case "chatOrientation" -> List.of(new DropdownOption("chatHorizontal", tr("evoassist.ui.horizontal")),
                    new DropdownOption("chatVertical", tr("evoassist.ui.vertical")));
            case "allowed" -> List.of(new DropdownOption("allowBlocks", tr("evoassist.ui.blocks")),
                    new DropdownOption("allowBarrels", tr("evoassist.ui.barrels")), new DropdownOption("allowRunes", tr("evoassist.ui.runes")),
                    new DropdownOption("allowBombs", tr("evoassist.ui.bombs")), new DropdownOption("allowPets", tr("evoassist.ui.pets")),
                    new DropdownOption("allowWands", tr("evoassist.ui.wands")), new DropdownOption("allowMultitool", tr("evoassist.ui.multitool")));
            default -> List.of();
        };
    }

    private DropdownBounds dropdownBounds() {
        if (openDropdown == null) return null;
        int rowY = contentTop() - scroll;
        for (Row row : rows()) {
            if (row.id.equals(openDropdown)) {
                int buttonTop = controlY(rowY) - 2;
                if (buttonTop < contentTop() || buttonTop + 22 > contentBottom()) return null;
                int popupHeight = dropdownOptions().size() * 18 + 4;
                int popupY = buttonTop + 22;
                if (popupY + popupHeight > height - 8) popupY = buttonTop - popupHeight;
                return new DropdownBounds(comboX(), Math.max(8, popupY), comboWidth(), popupHeight);
            }
            rowY += rowStep(row);
        }
        return null;
    }

    private void renderDropdown(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        DropdownBounds bounds = dropdownBounds();
        if (bounds == null) return;
        g.nextStratum();
        surface(g, bounds.x - 1, bounds.y - 1, bounds.x + bounds.width + 1,
                bounds.y + bounds.height + 1, 0xFF85919B);
        surface(g, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0xFA30363D);
        int y = bounds.y + 2;
        for (DropdownOption option : dropdownOptions()) {
            float amount = hoverAmount("option:" + openDropdown + ":" + option.id,
                    inside(mouseX, mouseY, bounds.x + 1, y, bounds.width - 2, 18));
            surface(g, bounds.x + 1, y, bounds.x + bounds.width - 1, y + 18,
                    mixColor(0xFA30363D, 0xFF556575, amount));
            surface(g, bounds.x + 1, y + 17, bounds.x + bounds.width - 1, y + 18,
                    mixColor(0xFF63717D, ACCENT, amount));
            boolean multiSelect = "allowed".equals(openDropdown);
            if (multiSelect && optionSelected(option.id)) g.text(font, "✓", bounds.x + 7, y + 5, TEXT);
            g.centeredText(font, fit(option.title, bounds.width - (multiSelect ? 36 : 16)),
                    bounds.x + bounds.width / 2, y + 5, TEXT);
            y += 18;
        }
    }

    private void renderRow(GuiGraphicsExtractor g, Row row, int y, int rowH, int mouseX, int mouseY) {
        if (row.kind == Kind.SECTION) {
            String label = fit(row.title, cardWidth() - 32);
            int center = cardX() + cardWidth() / 2;
            int halfText = (font.width(label) + 1) / 2;
            int lineY = y + rowH / 2;
            int leftEnd = center - halfText - 8;
            int rightStart = center + halfText + 8;
            surface(g, cardX(), lineY, leftEnd, lineY + 1, 0xFF81909E);
            surface(g, rightStart, lineY, cardX() + cardWidth(), lineY + 1, 0xFF81909E);
            g.centeredText(font, label, center, y + (rowH - 9) / 2, MUTED);
            return;
        }
        boolean hover = inside(mouseX, mouseY, cardX(), y, cardWidth(), rowH)
                && mouseY >= contentTop() && mouseY < contentBottom();
        surface(g, cardX(), y, cardX() + cardWidth(), y + rowH, hover ? CARD_HOVER : CARD);
        surface(g, cardX(), y, cardX() + 2, y + rowH, hover ? ACCENT : 0xFF8A98A5);

        int labelWidth = narrow() ? cardWidth() - 24 : controlX() - cardX() - 22;
        g.text(font, fit(row.title, labelWidth), cardX() + 12, y + 11, TEXT);
        if (!narrow()) {
            g.text(font, fit(row.hint, labelWidth), cardX() + 12, y + 27, MUTED);
        }
        int cy = controlY(y);
        int cx = controlX();
        int cw = controlWidth();

        if (row.kind == Kind.SLIDER || row.kind == Kind.CPS) {
            int current = value(row.id);
            int trackX = sliderStart(row);
            int trackWidth = sliderWidth(row);
            int filled = Math.round(trackWidth * Math.max(0, Math.min(1,
                    (current - row.min) / (float) (row.max - row.min))));
            renderControl(g, "track:" + row.id, trackX - 4, cy + 2, trackWidth + 8, 16, mouseX, mouseY);
            surface(g, trackX, cy + 8, trackX + trackWidth, cy + 12, TRACK);
            surface(g, trackX, cy + 8, trackX + filled, cy + 12, ACCENT);
            surface(g, trackX + filled - 2, cy + 5, trackX + filled + 2, cy + 15, TEXT);
            renderControl(g, "value:" + row.id, valueX(), cy - 2, 40, 22, mouseX, mouseY);
            String shown = row.id.equals(editingId) ? editingText + "_" : Integer.toString(current);
            g.centeredText(font, fit(shown, 36), valueX() + 20, cy + 5, TEXT);
            renderReset(g, row.id, cy, mouseX, mouseY);
            if (row.kind == Kind.CPS) {
                g.text(font, "1", cx, cy + 5, MUTED);
                g.text(font, "20", trackX + trackWidth + 10, cy + 5, MUTED);
            }
            return;
        }

        if (row.kind == Kind.NUMBER || row.kind == Kind.AMOUNT) {
            int boxX = cx + cw - 110;
            renderControl(g, "value:" + row.id, boxX, cy - 2, 68, 22, mouseX, mouseY);
            String shown = row.id.equals(editingId) ? editingText + "_"
                    : row.kind == Kind.AMOUNT ? amountLabel(row.id) : Integer.toString(value(row.id));
            g.centeredText(font, fit(shown, 62), boxX + 34, cy + 5, TEXT);
            renderReset(g, row.id, cy, mouseX, mouseY);
            return;
        }

        if (row.kind == Kind.TOGGLE) {
            renderToggle(g, row.id, enabled(row.id), cx + cw - 32, cy + 2, mouseX, mouseY);
            return;
        }
        if (row.kind == Kind.ACTION) {
            renderResetButton(g, row.id, cx, cy - 2, cw, tr("evoassist.ui.resetAction"), mouseX, mouseY);
            return;
        }
        if (row.kind == Kind.EDITOR) {
            renderControl(g, "editor:" + row.id, cx, cy - 2, cw, 22, mouseX, mouseY);
            g.centeredText(font, tr("evoassist.ui.configure"), cx + cw / 2, cy + 5, TEXT);
            return;
        }

        String label = switch (row.kind) {
            case CHOICE -> choice(row.id);
            case MULTI -> tr("evoassist.ui.select");
            case KEY -> row.id.equals(capturingKeyId) ? tr("evoassist.ui.pressKey") : bindingFor(row.id)
                    .getTranslatedKeyMessage().getString();
            default -> "";
        };
        if (row.kind == Kind.CHOICE || row.kind == Kind.MULTI) {
            cx = comboX();
            cw = comboWidth();
            renderControl(g, "combo:" + row.id, cx, cy - 2, cw, 22, mouseX, mouseY);
            g.centeredText(font, fit(label, cw - 30), cx + cw / 2, cy + 5, TEXT);
            g.text(font, row.id.equals(openDropdown) ? "▲" : "▼", cx + cw - 13, cy + 5, TEXT);
            return;
        }
        renderControl(g, "key:" + row.id, cx, cy - 2, cw, 22, mouseX, mouseY);
        g.centeredText(font, fit(label, cw - 8), cx + cw / 2, cy + 5, TEXT);
    }

    private void renderToggle(GuiGraphicsExtractor g, String id, boolean enabled, int x, int y,
                              int mouseX, int mouseY) {
        renderControl(g, "toggle:" + id, x, y, 32, 14, mouseX, mouseY);
        surface(g, x + 1, y + 1, x + 31, y + 13, enabled ? 0xFF2984C7 : 0xFF34414D);
        int handleX = enabled ? x + 19 : x + 2;
        surface(g, handleX, y + 2, handleX + 11, y + 12, 0xFFE8F4FC);
    }

    private void renderReset(GuiGraphicsExtractor g, String id, int cy, int mouseX, int mouseY) {
        renderResetButton(g, id, resetX(), cy - 2, 39, tr("evoassist.ui.reset"), mouseX, mouseY);
    }

    private void renderResetButton(GuiGraphicsExtractor g, String id, int x, int y, int width, String label,
                                   int mouseX, int mouseY) {
        renderControl(g, "reset:" + id, x, y, width, 22, mouseX, mouseY);
        long elapsed = System.currentTimeMillis() - resetFlashAt;
        boolean flashing = id.equals(resetFlashId) && elapsed >= 0 && elapsed < RESET_FLASH_DURATION_MS;
        if (flashing) {
            int alpha = (int) (220 * (RESET_FLASH_DURATION_MS - elapsed) / RESET_FLASH_DURATION_MS);
            surface(g, x, y, x + width, y + 22, (alpha << 24) | (ACCENT & 0x00FFFFFF));
        }
        g.centeredText(font, flashing ? "✓" : label, x + width / 2, y + 7, TEXT);
    }

    private int resetDialogWidth() { return Math.min(300, width - 24); }
    private int resetDialogX() { return (width - resetDialogWidth()) / 2; }
    private int resetDialogY() { return (height - 92) / 2; }
    private int resetDialogButtonWidth() { return (resetDialogWidth() - 30) / 2; }

    private void renderResetConfirmation(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.nextStratum();
        g.fill(0, 0, width, height, 0xAA000000);
        int x = resetDialogX();
        int y = resetDialogY();
        int w = resetDialogWidth();
        g.fill(x, y, x + w, y + 92, 0xEE35404B);
        g.fill(x, y, x + w, y + 1, 0xFF91A2B1);
        g.fill(x, y + 91, x + w, y + 92, 0xFF91A2B1);
        g.fill(x, y, x + 1, y + 92, 0xFF91A2B1);
        g.fill(x + w - 1, y, x + w, y + 92, 0xFF91A2B1);
        g.centeredText(font, tr("evoassist.ui.confirmReset"), width / 2, y + 14, TEXT);
        String target = switch (pendingResetId) {
            case "resetMining" -> tr("evoassist.ui.resetMining.subject");
            case "resetGoals" -> tr("evoassist.ui.resetGoals.subject");
            case "resetClanGoals" -> tr("evoassist.ui.resetClanGoals.subject");
            case "resetBosses" -> tr("evoassist.ui.resetBosses.subject");
            case "resetClan" -> tr("evoassist.ui.resetClan.subject");
            default -> rows().stream().filter(row -> row.id.equals(pendingResetId))
                    .map(row -> row.title).findFirst().orElse(tr("evoassist.ui.setting"));
        };
        g.centeredText(font, fit(target, w - 20), width / 2, y + 33, MUTED);
        int buttonWidth = resetDialogButtonWidth();
        renderDialogButton(g, "cancelReset", tr("evoassist.ui.cancel"), x + 10, y + 58, buttonWidth, mouseX, mouseY);
        renderDialogButton(g, "confirmReset", tr("evoassist.ui.resetAction"), x + 20 + buttonWidth, y + 58,
                buttonWidth, mouseX, mouseY);
    }

    private void renderDialogButton(GuiGraphicsExtractor g, String id, String label,
                                    int x, int y, int w, int mouseX, int mouseY) {
        float amount = hoverAmount(id, inside(mouseX, mouseY, x, y, w, 24));
        g.fill(x, y, x + w, y + 24, mixColor(0xFF405061, 0xFF5B7183, amount));
        int border = mixColor(0xFF91A2B1, ACCENT, amount);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + 23, x + w, y + 24, border);
        g.fill(x, y, x + 1, y + 24, border);
        g.fill(x + w - 1, y, x + w, y + 24, border);
        g.centeredText(font, label, x + w / 2, y + 8, TEXT);
    }

    private void requestReset(String id) {
        pendingResetId = id;
        openDropdown = null;
    }

    private void confirmReset() {
        String id = pendingResetId;
        pendingResetId = null;
        if (id == null) return;
        if (id.equals("resetMining")) resetMining();
        else if (id.equals("resetGoals")) resetGoals();
        else if (id.equals("resetClanGoals")) {
            AdditionalGoals.getInstance().reset(true);
            MiningGoals.getInstance().clearNotices(true);
            feedback = tr("evoassist.ui.clanGoalsReset");
            feedbackUntil = System.currentTimeMillis() + 2500;
            startResetFlash(id);
        }
        else if (id.equals("resetBosses") || id.equals("resetClan")) resetRewards(id);
        else resetValue(id);
    }

    private void startResetFlash(String id) {
        resetFlashId = id;
        resetFlashAt = System.currentTimeMillis();
    }

    private static boolean enabled(String id) {
        return switch (id) {
            case "clickerEnabled" -> ConfigAutoclicker.autoclickerEnabled;
            case "miningWidget" -> ConfigMining.bphWidgetToggle;
            case "blockGoalWidget" -> ConfigMining.blockGoalWidgetEnabled;
            case "timeGoalWidget" -> ConfigMining.timeGoalWidgetEnabled;
            case "moneyGoalWidget" -> ConfigMining.moneyGoalWidgetEnabled;
            case "shardGoalWidget" -> ConfigMining.shardGoalWidgetEnabled;
            case "clanPointsGoalWidget" -> ConfigClan.pointsGoalWidgetEnabled;
            case "clanGoldGoalWidget" -> ConfigClan.goldGoalWidgetEnabled;
            case "clanExperienceGoalWidget" -> ConfigClan.experienceGoalWidgetEnabled;
            case "clanGoalNotifications" -> ConfigClan.goalNotifications;
            case "goalNotifications" -> ConfigMining.goalNotifications;
            case "bossWidget" -> ConfigBosses.widgetEnabled;
            case "clanWidget" -> ConfigClan.widgetEnabled;
            case "chatTabs" -> ConfigChat.chatTabsToggle;
            default -> false;
        };
    }

    private static ConfigMining.bphAllowEnum allowedType(String id) {
        return switch (id) {
            case "allowBlocks" -> ConfigMining.bphAllowEnum.BLOCKS;
            case "allowBarrels" -> ConfigMining.bphAllowEnum.BARRELS;
            case "allowRunes" -> ConfigMining.bphAllowEnum.RUNES;
            case "allowBombs" -> ConfigMining.bphAllowEnum.BOMBS;
            case "allowPets" -> ConfigMining.bphAllowEnum.PETS;
            case "allowWands" -> ConfigMining.bphAllowEnum.WANDS;
            case "allowMultitool" -> ConfigMining.bphAllowEnum.MULTITOOL;
            default -> null;
        };
    }

    private static boolean optionSelected(String id) {
        ConfigMining.bphAllowEnum type = allowedType(id);
        return type != null && Arrays.asList(ConfigMining.bphWidgetAllowed).contains(type);
    }

    private static String choice(String id) {
        return switch (id) {
            case "clickerButton" -> ConfigAutoclicker.autoclickerButton.toString();
            case "clickerMode" -> ConfigAutoclicker.autoclickerActivation.toString();
            case "chatOrientation" -> ConfigChat.chatTabsOrientation.toString();
            default -> "";
        };
    }

    private static KeyMapping bindingFor(String id) {
        return switch (id) {
            case "menuKey" -> Evo_assistClient.menuKey();
            case "clickerModeKey" -> Evo_assistClient.clickerModeKey();
            default -> Evo_assistClient.clickerKey();
        };
    }

    private static int value(String id) {
        return switch (id) {
            case "cps" -> ConfigAutoclicker.autoclickerCps;
            case "menuOpacity" -> ConfigVisual.menuOpacity;
            case "goalNoticeDuration" -> ConfigVisual.goalNoticeDurationSeconds;
            case "blockGoal" -> ConfigMining.blockGoalTarget;
            case "timeGoal" -> ConfigMining.timeGoalMinutes;
            default -> 0;
        };
    }

    private static void setValue(String id, int newValue) {
        switch (id) {
            case "cps" -> ConfigAutoclicker.autoclickerCps = newValue;
            case "menuOpacity" -> ConfigVisual.menuOpacity = Math.max(10, Math.min(100, newValue));
            case "goalNoticeDuration" -> ConfigVisual.goalNoticeDurationSeconds = Math.max(1, Math.min(60, newValue));
            case "blockGoal" -> {
                ConfigMining.blockGoalTarget = newValue;
                MiningGoals.getInstance().syncTargets();
            }
            case "timeGoal" -> {
                ConfigMining.timeGoalMinutes = Math.max(0, Math.min(9_999, newValue));
                MiningGoals.getInstance().syncTargets();
            }
        }
    }

    private void resetValue(String id) {
        if (amountType(id) != null) setAmount(id, "0");
        switch (id) {
            case "cps" -> setValue(id, 10);
            case "menuOpacity" -> setValue(id, 90);
            case "goalNoticeDuration" -> setValue(id, 5);
            case "blockGoal", "timeGoal" -> setValue(id, 0);
        }
        startResetFlash(id);
        editingId = null;
        feedback = tr("evoassist.ui.defaultRestored");
        feedbackUntil = System.currentTimeMillis() + 2500;
        save();
    }

    private void toggle(String id) {
        switch (id) {
            case "clickerEnabled" -> {
                ConfigAutoclicker.autoclickerEnabled = !ConfigAutoclicker.autoclickerEnabled;
                if (!ConfigAutoclicker.autoclickerEnabled) Clicker.stop();
            }
            case "miningWidget" -> ConfigMining.bphWidgetToggle = !ConfigMining.bphWidgetToggle;
            case "blockGoalWidget" -> ConfigMining.blockGoalWidgetEnabled = !ConfigMining.blockGoalWidgetEnabled;
            case "timeGoalWidget" -> ConfigMining.timeGoalWidgetEnabled = !ConfigMining.timeGoalWidgetEnabled;
            case "moneyGoalWidget" -> ConfigMining.moneyGoalWidgetEnabled = !ConfigMining.moneyGoalWidgetEnabled;
            case "shardGoalWidget" -> ConfigMining.shardGoalWidgetEnabled = !ConfigMining.shardGoalWidgetEnabled;
            case "clanPointsGoalWidget" -> ConfigClan.pointsGoalWidgetEnabled = !ConfigClan.pointsGoalWidgetEnabled;
            case "clanGoldGoalWidget" -> ConfigClan.goldGoalWidgetEnabled = !ConfigClan.goldGoalWidgetEnabled;
            case "clanExperienceGoalWidget" -> ConfigClan.experienceGoalWidgetEnabled = !ConfigClan.experienceGoalWidgetEnabled;
            case "clanGoalNotifications" -> ConfigClan.goalNotifications = !ConfigClan.goalNotifications;
            case "goalNotifications" -> ConfigMining.goalNotifications = !ConfigMining.goalNotifications;
            case "bossWidget" -> ConfigBosses.widgetEnabled = !ConfigBosses.widgetEnabled;
            case "clanWidget" -> ConfigClan.widgetEnabled = !ConfigClan.widgetEnabled;
            case "chatTabs" -> {
                ConfigChat.chatTabsToggle = !ConfigChat.chatTabsToggle;
                minecraft.gui.hud.getChat().rescaleChat();
            }
        }
        save();
    }

    private void selectOption(String id) {
        switch (id) {
            case "chatHorizontal" -> ConfigChat.chatTabsOrientation = ConfigChat.Orientation.HORIZONTAL;
            case "chatVertical" -> ConfigChat.chatTabsOrientation = ConfigChat.Orientation.VERTICAL;
            case "buttonLeft" -> ConfigAutoclicker.autoclickerButton = ConfigAutoclicker.ENUMautoclickerButton.LMB;
            case "buttonRight" -> ConfigAutoclicker.autoclickerButton = ConfigAutoclicker.ENUMautoclickerButton.RMB;
            case "modeClick" -> {
                Clicker.stop();
                ConfigAutoclicker.autoclickerActivation = ConfigAutoclicker.ENUMautoclickerActivation.SWITCH;
            }
            case "modeHold" -> {
                Clicker.stop();
                ConfigAutoclicker.autoclickerActivation = ConfigAutoclicker.ENUMautoclickerActivation.HOLD;
            }
            default -> {
                ConfigMining.bphAllowEnum type = allowedType(id);
                if (type != null) {
                    List<ConfigMining.bphAllowEnum> selected =
                            new ArrayList<>(Arrays.asList(ConfigMining.bphWidgetAllowed));
                    if (!selected.remove(type)) selected.add(type);
                    ConfigMining.bphWidgetAllowed = selected.toArray(new ConfigMining.bphAllowEnum[0]);
                }
            }
        }
        if (!"allowed".equals(openDropdown)) openDropdown = null;
        save();
    }

    private void resetMining() {
        BlockProfitPerHour counter = BlockProfitPerHour.getInstance();
        if (counter != null) {
            counter.reset();
            feedback = tr("evoassist.ui.miningReset");
            startResetFlash("resetMining");
        } else {
            feedback = tr("evoassist.ui.miningUnavailable");
        }
        feedbackUntil = System.currentTimeMillis() + 2500;
    }

    private void resetGoals() {
        MiningGoals.getInstance().resetAll();
        feedback = tr("evoassist.ui.goalsReset");
        feedbackUntil = System.currentTimeMillis() + 2500;
        startResetFlash("resetGoals");
    }

    private void resetRewards(String id) {
        if (!Evo_assistClient.rewardStatistics.hasServer()) {
            feedback = tr("evoassist.ui.connectFirst");
        } else {
            if (id.equals("resetBosses")) Evo_assistClient.rewardStatistics.resetBosses();
            else Evo_assistClient.rewardStatistics.resetClan();
            feedback = tr("evoassist.ui.statisticsReset");
            startResetFlash(id);
        }
        feedbackUntil = System.currentTimeMillis() + 2500;
    }

    private void updateSlider(Row row, double mouseX) {
        double fraction = Math.max(0, Math.min(1, (mouseX - sliderStart(row)) / sliderWidth(row)));
        setValue(row.id, row.min + (int) Math.round(fraction * (row.max - row.min)));
        editingId = null;
    }

    private void startEditing(String id) {
        editingId = id;
        editingText = "";
    }

    private static AdditionalGoals.Type amountType(String id) {
        return switch (id) {
            case "moneyGoal" -> AdditionalGoals.Type.MONEY;
            case "shardGoal" -> AdditionalGoals.Type.SHARDS;
            case "clanPointsGoal" -> AdditionalGoals.Type.CLAN_POINTS;
            case "clanGoldGoal" -> AdditionalGoals.Type.CLAN_GOLD;
            case "clanExperienceGoal" -> AdditionalGoals.Type.CLAN_EXPERIENCE;
            default -> null;
        };
    }

    private static String amountLabel(String id) {
        var type = amountType(id);
        try {
            return AdditionalGoals.format(type, GoalAmount.parse(
                    AdditionalGoals.configuredTarget(type), type.whole()).min(type.maximum()));
        } catch (IllegalArgumentException error) { return "0"; }
    }

    private static void setAmount(String id, String input) {
        var type = amountType(id);
        String value = AdditionalGoals.parseTarget(type, input).toPlainString();
        AdditionalGoals.setConfiguredTarget(type, value);
        AdditionalGoals.getInstance().syncTargets();
    }

    private void commitEditing() {
        if (editingId == null) return;
        String id = editingId;
        try {
            if (!editingText.isEmpty()) {
                if (amountType(id) != null) setAmount(id, editingText);
                else {
                    int typed = Integer.parseInt(editingText);
                    Row row = rows().stream().filter(item -> item.id.equals(id)).findFirst().orElse(null);
                    if (row != null) setValue(id, Math.max(row.min, Math.min(row.max, typed)));
                }
            }
        } catch (IllegalArgumentException error) {
            feedback = amountType(id) == null ? tr("evoassist.ui.integerRequired") : error.getMessage();
            feedbackUntil = System.currentTimeMillis() + 4000;
        }
        editingId = null;
        save();
    }

    private void save() {
        Evo_assistClient.configurator.saveConfig(Config.class);
    }

    private void prepareControlClick() {
        commitEditing();
        capturingKeyId = null;
        openDropdown = null;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (pendingResetId != null) {
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                int x = resetDialogX();
                int y = resetDialogY() + 58;
                int buttonWidth = resetDialogButtonWidth();
                if (inside(event.x(), event.y(), x + 10, y, buttonWidth, 24)) {
                    pendingResetId = null;
                } else if (inside(event.x(), event.y(), x + 20 + buttonWidth, y, buttonWidth, 24)) {
                    confirmReset();
                }
            }
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
        double mx = event.x();
        double my = event.y();

        DropdownBounds popup = dropdownBounds();
        if (popup != null && inside(mx, my, popup.x, popup.y, popup.width, popup.height)) {
            int optionIndex = ((int) my - popup.y - 2) / 18;
            List<DropdownOption> options = dropdownOptions();
            if (my >= popup.y + 2 && my < popup.y + popup.height - 2
                    && optionIndex >= 0 && optionIndex < options.size()) {
                commitEditing();
                capturingKeyId = null;
                selectOption(options.get(optionIndex).id);
            }
            return true;
        }

        if (inside(mx, my, widgetEditorButtonX(), widgetEditorButtonY(), 20, 20)) {
            prepareControlClick();
            save();
            minecraft.gui.setScreen(new WidgetScreen(this));
            return true;
        }
        int navY = top() + 48;
        for (Page item : Page.values()) {
            if (inside(mx, my, left() + 7, navY, sidebarWidth() - 14, navHeight())) {
                prepareControlClick();
                page = item;
                scroll = 0;
                return true;
            }
            navY += navStep();
        }
        if (inside(mx, my, right() - 69, bottom() - 31, 60, 24)) {
            prepareControlClick();
            onClose();
            return true;
        }
        if (!inside(mx, my, contentX(), contentTop(), contentWidth(), contentBottom() - contentTop())) {
            return true;
        }

        int y = contentTop() - scroll;
        for (Row row : rows()) {
            int rowH = rowHeight(row);
            if (inside(mx, my, cardX(), y, cardWidth(), rowH)) {
                int cy = controlY(y);
                int cx = controlX();
                int cw = controlWidth();
                switch (row.kind) {
                    case SECTION -> { }
                    case TOGGLE -> {
                        if (inside(mx, my, cx + cw - 32, cy + 2, 32, 14)) {
                            prepareControlClick();
                            toggle(row.id);
                        }
                    }
                    case CHOICE, MULTI -> {
                        if (inside(mx, my, comboX(), cy - 2, comboWidth(), 22)) {
                            String previousDropdown = openDropdown;
                            prepareControlClick();
                            openDropdown = row.id.equals(previousDropdown) ? null : row.id;
                        }
                    }
                    case KEY -> {
                        if (inside(mx, my, cx, cy - 2, cw, 22)) {
                            prepareControlClick();
                            capturingKeyId = row.id;
                        }
                    }
                    case ACTION -> {
                        if (inside(mx, my, cx, cy - 2, cw, 22)) {
                            prepareControlClick();
                            requestReset(row.id);
                        }
                    }
                    case EDITOR -> {
                        if (inside(mx, my, cx, cy - 2, cw, 22)) {
                            prepareControlClick();
                            save();
                            minecraft.gui.setScreen(new WidgetScreen(this));
                        }
                    }
                    case NUMBER, AMOUNT -> {
                        int boxX = cx + cw - 110;
                        if (inside(mx, my, resetX(), cy - 2, 39, 22)) {
                            prepareControlClick();
                            requestReset(row.id);
                        } else if (inside(mx, my, boxX, cy - 2, 68, 22)) {
                            prepareControlClick();
                            startEditing(row.id);
                        }
                    }
                    case SLIDER, CPS -> {
                        if (inside(mx, my, resetX(), cy - 2, 39, 22)) {
                            prepareControlClick();
                            requestReset(row.id);
                        } else if (inside(mx, my, valueX(), cy - 2, 40, 22)) {
                            prepareControlClick();
                            startEditing(row.id);
                        } else if (inside(mx, my, sliderStart(row) - 4, cy + 2, sliderWidth(row) + 8, 16)) {
                            prepareControlClick();
                            draggingSlider = row;
                            updateSlider(row, mx);
                        }
                    }
                }
                return true;
            }
            y += rowStep(row);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (pendingResetId != null) return true;
        if (draggingSlider != null) {
            updateSlider(draggingSlider, event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingSlider != null) {
            draggingSlider = null;
            save();
            return true;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
        if (pendingResetId != null) return true;
        if (inside(mouseX, mouseY, contentX(), contentTop(), contentWidth(), contentBottom() - contentTop())) {
            openDropdown = null;
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(yAmount * 28)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, xAmount, yAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pendingResetId != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) pendingResetId = null;
            else if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) confirmReset();
            return true;
        }
        if (capturingKeyId != null) {
            String keyId = capturingKeyId;
            capturingKeyId = null;
            if (event.key() != GLFW.GLFW_KEY_ESCAPE) {
                KeyMapping mapping = bindingFor(keyId);
                mapping.setKey(event.key() == GLFW.GLFW_KEY_DELETE
                        ? InputConstants.UNKNOWN : InputConstants.getKey(event));
                KeyMapping.resetMapping();
                minecraft.options.save();
            }
            return true;
        }
        if (editingId != null) {
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER
                    || event.key() == GLFW.GLFW_KEY_TAB) {
                commitEditing();
            } else if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                editingId = null;
            } else if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !editingText.isEmpty()) {
                editingText = editingText.substring(0, editingText.length() - 1);
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (openDropdown != null) {
                openDropdown = null;
                return true;
            }
            onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN) {
            int next = page.ordinal() + (event.key() == GLFW.GLFW_KEY_DOWN ? 1 : -1);
            page = Page.values()[Math.floorMod(next, Page.values().length)];
            scroll = 0;
            openDropdown = null;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (pendingResetId != null) return true;
        if (editingId != null) {
            int cp = event.codepoint();
            boolean amount = amountType(editingId) != null;
            boolean allowed = cp >= '0' && cp <= '9' || amount && (cp == '.' || "KMBTQkmbtq".indexOf(cp) >= 0);
            int maxLength = editingId.equals("timeGoal") ? 4 : amount ? 32 : 7;
            if (allowed && editingText.length() < maxLength) {
                editingText += (char) cp;
            }
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        commitEditing();
        save();
        minecraft.gui.setScreen(parent);
    }
}
