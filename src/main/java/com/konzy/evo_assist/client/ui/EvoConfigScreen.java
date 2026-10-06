package com.konzy.evo_assist.client.ui;

import static com.konzy.evo_assist.client.util.Texts.tr;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.Config;
import com.konzy.evo_assist.client.config.ConfigCalculator;
import com.konzy.evo_assist.client.features.calculator.LevelCalculator;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import com.konzy.evo_assist.client.util.GoalAmount;
import com.konzy.evo_assist.client.util.TimeUtils;
import com.konzy.evo_assist.client.features.rewards.RewardMessageParser;
import com.konzy.evo_assist.client.config.ConfigChat;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.config.ConfigVisual;
import com.konzy.evo_assist.client.config.ConfigClan;
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
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

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
    private Page page = Page.MINING;
    private int scroll;
    private Row draggingSlider;
    private String openDropdown;
    private String capturingKeyId;
    private String editingId;
    private String editingText = "";
    private boolean calculatorSelectAll;
    private String feedback;
    private long feedbackUntil;
    private String resetFlashId;
    private long resetFlashAt;
    private String pendingResetId;
    private record GoalChange(String id, String target) {}
    private GoalChange pendingGoalChange;
    private int confirmationScroll;
    private List<FormattedCharSequence> confirmationLines = List.of();
    private static final long RESET_FLASH_DURATION_MS = 650;
    private final Map<String, Float> hoverProgress = new HashMap<>();
    private long lastHoverFrameNanos;
    private float hoverStep = 1.0f;

    private enum Page {
        BOSSES("evoassist.page.bosses.title", "evoassist.page.bosses.subtitle"),
        KILLS("evoassist.page.kills.title", "evoassist.page.kills.subtitle"),
        MINING("evoassist.page.mining.title", "evoassist.page.mining.subtitle"),
        MINING_GOALS("evoassist.page.mining_goals.title", "evoassist.page.mining_goals.subtitle"),
        CLAN("evoassist.page.clan.title", "evoassist.page.clan.subtitle"),
        CLAN_GOALS("evoassist.page.clan_goals.title", "evoassist.page.clan_goals.subtitle"),
        CALCULATOR("evoassist.page.calculator.title", "evoassist.page.calculator.subtitle"),
        INTERFACE("evoassist.page.interface.title", "evoassist.page.interface.subtitle");

        final String title;
        final String subtitle;

        Page(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private enum Kind { TOGGLE, CHOICE, MULTI, SLIDER, NUMBER, AMOUNT, KEY, ACTION, SECTION, LEVELS, RESULT, INFO }

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
    }

    public EvoConfigScreen(Screen parent) {
        super(Component.literal("EvoAssist"));
        this.parent = parent;
        String fullVersion = FabricLoader.getInstance().getModContainer(EvoAssistClient.MODID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("dev");
        this.modVersion = fullVersion.split("\\+")[0];
        ConfigVisual.menuOpacity = Math.clamp(ConfigVisual.menuOpacity, 10, 100);
        ConfigVisual.goalNoticeDurationSeconds = Math.clamp(ConfigVisual.goalNoticeDurationSeconds, 1, 60);
    }

    private List<Row> rows() {
        List<Row> items = new ArrayList<>();
        switch (page) {
            case MINING -> {
                items.add(new Row("allowed", tr("evoassist.ui.allowed.title"), tr("evoassist.ui.allowed.hint"), Kind.MULTI, 0, 0));
                items.add(Row.action("resetMining", tr("evoassist.ui.resetMining.title"), tr("evoassist.ui.resetMining.hint")));
            }
            case MINING_GOALS -> {
                items.add(Row.section(tr("evoassist.section.blocks")));
                items.add(Row.number("blockGoal", tr("evoassist.ui.blockGoal.title"), tr("evoassist.ui.blockGoal.hint"), 0, 9_999_999));
                items.add(Row.action("blockGoal", tr("evoassist.ui.reset.blockGoal.title"), tr("evoassist.ui.resetGoal.hint")));
                items.add(Row.section(tr("evoassist.section.time")));
                items.add(Row.number("timeGoal", tr("evoassist.ui.timeGoal.title"), tr("evoassist.ui.timeGoal.hint"), 0, 9_999));
                items.add(Row.action("timeGoal", tr("evoassist.ui.reset.timeGoal.title"), tr("evoassist.ui.resetGoal.hint")));
                items.add(Row.section(tr("evoassist.section.money")));
                items.add(Row.amount("moneyGoal", tr("evoassist.ui.moneyGoal.title"), tr("evoassist.ui.moneyGoal.hint")));
                items.add(Row.action("moneyGoal", tr("evoassist.ui.reset.moneyGoal.title"), tr("evoassist.ui.resetGoal.hint")));
                items.add(Row.section(tr("evoassist.section.shards")));
                items.add(Row.amount("shardGoal", tr("evoassist.ui.shardGoal.title"), tr("evoassist.ui.shardGoal.hint")));
                items.add(Row.action("shardGoal", tr("evoassist.ui.reset.shardGoal.title"), tr("evoassist.ui.resetGoal.hint")));
                items.add(Row.section(tr("evoassist.section.general")));
                items.add(Row.toggle("goalNotifications", tr("evoassist.ui.goalNotifications.title"), tr("evoassist.ui.goalNotifications.hint")));
                items.add(Row.slider("goalNoticeDuration", tr("evoassist.ui.goalNoticeDuration.title"), tr("evoassist.ui.goalNoticeDuration.hint"), 1, 60));
                items.add(Row.action("resetGoals", tr("evoassist.ui.resetGoals.title"), tr("evoassist.ui.resetGoals.hint")));
            }
            case BOSSES -> {
                items.add(Row.section(tr("evoassist.section.bossRewards")));
                items.add(Row.action("resetBossRewards", tr("evoassist.ui.resetRewards.title"), tr("evoassist.ui.resetBossRewards.hint")));
                items.add(Row.section(tr("evoassist.section.bossTokens")));
                items.add(Row.action("resetBossTokens", tr("evoassist.ui.resetRewards.title"), tr("evoassist.ui.resetBossTokens.hint")));
            }
            case CLAN -> {
                items.add(Row.action("resetClan", tr("evoassist.ui.resetRewards.title"), tr("evoassist.ui.resetClan.hint")));
            }
            case CLAN_GOALS -> {
                items.add(Row.section(tr("evoassist.section.clanPoints")));
                items.add(Row.amount("clanPointsGoal", tr("evoassist.ui.clanPointsGoal.title"), tr("evoassist.ui.clanPointsGoal.hint")));
                items.add(Row.action("clanPointsGoal", tr("evoassist.ui.reset.clanPointsGoal.title"), tr("evoassist.ui.resetGoal.hint")));
                items.add(Row.section(tr("evoassist.section.clanGold")));
                items.add(Row.amount("clanGoldGoal", tr("evoassist.ui.clanGoldGoal.title"), tr("evoassist.ui.clanGoldGoal.hint")));
                items.add(Row.action("clanGoldGoal", tr("evoassist.ui.reset.clanGoldGoal.title"), tr("evoassist.ui.resetGoal.hint")));
                items.add(Row.section(tr("evoassist.section.clanExperience")));
                items.add(Row.amount("clanExperienceGoal", tr("evoassist.ui.clanExperienceGoal.title"), tr("evoassist.ui.clanExperienceGoal.hint")));
                items.add(Row.action("clanExperienceGoal", tr("evoassist.ui.reset.clanExperienceGoal.title"), tr("evoassist.ui.resetGoal.hint")));
                items.add(Row.section(tr("evoassist.section.general")));
                items.add(Row.toggle("clanGoalNotifications", tr("evoassist.ui.goalNotifications.title"), tr("evoassist.ui.clanGoalNotifications.hint")));
                items.add(Row.slider("goalNoticeDuration", tr("evoassist.ui.goalNoticeDuration.title"), tr("evoassist.ui.goalNoticeDuration.hint"), 1, 60));
                items.add(Row.action("resetClanGoals", tr("evoassist.ui.resetGoals.title"), tr("evoassist.ui.resetClanGoals.hint")));
            }
            case CALCULATOR -> {
                items.add(Row.choice("calculatorMode", tr("evoassist.calculator.mode"), tr("evoassist.calculator.modeHint")));
                items.add(new Row("calculatorLevels", "", "", Kind.LEVELS, 1, LevelCalculator.MAX_LEVEL));
                items.add(Row.choice("calculatorDiscount", tr("evoassist.calculator.discount"), tr("evoassist.calculator.discountHint")));
                items.add(Row.section(tr("evoassist.calculator.result")));
                String error = calculatorError();
                if (error != null) items.add(new Row("calculatorStatus", error, "", Kind.INFO, 0, 0));
                else {
                    items.add(new Row("calculatorDifference", tr("evoassist.calculator.difference"), "", Kind.RESULT, 0, 0));
                    items.add(new Row("calculatorBlocks", tr("evoassist.calculator.blocks"), "", Kind.RESULT, 0, 0));
                    items.add(new Row("calculatorMoney", tr("evoassist.calculator.money"), "", Kind.RESULT, 0, 0));
                }
            }
            case KILLS -> {
                items.add(Row.action("resetKills", tr("evoassist.ui.resetKills.title"), tr("evoassist.ui.resetKills.hint")));
            }
            case INTERFACE -> {
                items.add(Row.section(tr("evoassist.section.chat")));
                items.add(Row.choice("chatOrientation", tr("evoassist.ui.chatOrientation.title"), tr("evoassist.ui.chatOrientation.hint")));
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
    private int sidebarWidth() { return Math.clamp(panelWidth() / 4, 94, 148); }
    private int contentX() { return left() + sidebarWidth() + 8; }
    private int contentWidth() { return right() - contentX(); }
    private int contentTop() { return top() + 48; }
    private int contentBottom() { return bottom() - 38; }
    private int navStep() { return Math.clamp((panelHeight() - 56) / Page.values().length, 12, 29); }
    private int navHeight() { return Math.min(24, navStep() - 2); }
    private int rowHeight(Row row) {
        if (row.kind == Kind.SECTION) return 24;
        if (row.kind == Kind.LEVELS) return cardWidth() < 280 ? 106 : 64;
        if (row.kind == Kind.RESULT) return narrow() ? 46 : 32;
        if (row.kind == Kind.INFO) return 69;
        return contentWidth() < 340 ? 69 : 56;
    }
    private int rowStep(Row row) { return rowHeight(row) + 7; }

    private int maxScroll() {
        int total = 0;
        for (Row row : rows()) total += rowStep(row);
        return Math.max(0, total - (contentBottom() - contentTop()));
    }

    private int menuColor(int color) {
        int opacity = Math.clamp(ConfigVisual.menuOpacity, 10, 100);
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
        renderControl(g, id, x, y, w, h, mouseX, mouseY, true);
    }

    private void renderControl(GuiGraphicsExtractor g, String id, int x, int y, int w, int h,
                               int mouseX, int mouseY, boolean active) {
        boolean hovered = active && inside(mouseX, mouseY, x, y, w, h)
                && (id.equals("done") || (mouseY >= contentTop() && mouseY < contentBottom()));
        float amount = hoverAmount(id, hovered);
        surface(g, x, y, x + w, y + h, active ? mixColor(CONTROL, 0xF05B7183, amount) : 0xFF303A43);
        int border = active ? mixColor(0xFF8594A1, ACCENT, amount) : 0xFF596671;
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
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // This screen draws its own adjustable translucent backdrop.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        if (lastHoverFrameNanos != 0) {
            hoverStep = Math.clamp((now - lastHoverFrameNanos) / 100_000_000.0f, 0.0f, 1.0f);
        }
        lastHoverFrameNanos = now;
        scroll = Math.min(scroll, maxScroll());
        surface(g, 0, 0, width, height, BACKDROP);
        surface(g, left(), top(), right(), bottom(), PANEL);
        surface(g, left(), top(), left() + sidebarWidth(), bottom(), SIDEBAR);
        surface(g, left() + sidebarWidth(), top(), left() + sidebarWidth() + 1, bottom(), 0xFF83919E);

        g.text(font, "EvoAssist", left() + 12, top() + 11, TEXT);
        g.text(font, modVersion, left() + 12, top() + 26, MUTED);

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
                ? feedback : tr(page == Page.CALCULATOR ? "evoassist.calculator.auto" : "evoassist.ui.saveHint");
        g.text(font, fit(footer, contentWidth() - 90), contentX() + 10, bottom() - 22, MUTED);
        renderControl(g, "done", right() - 69, bottom() - 31, 60, 24, mouseX, mouseY);
        g.centeredText(font, tr("evoassist.ui.done"), right() - 39, bottom() - 23, TEXT);
        renderDropdown(g, mouseX, mouseY);
        if (hasConfirmation()) renderResetConfirmation(g, mouseX, mouseY);
    }

    private int cardX() { return contentX() + 8; }
    private int cardWidth() { return contentWidth() - 16; }
    private boolean narrow() { return contentWidth() < 340; }
    private int controlWidth() { return narrow() ? cardWidth() - 24 : Math.clamp(cardWidth() / 3, 155, 192); }
    private int controlX() { return narrow() ? cardX() + 12 : cardX() + cardWidth() - controlWidth() - 12; }
    private int controlY(int rowY) { return narrow() ? rowY + 37 : rowY + 16; }
    private int resetX() { return controlX() + controlWidth() - 39; }
    private int valueX() { return resetX() - 44; }
    private int sliderStart(Row row) { return controlX(); }
    private int sliderWidth(Row row) {
        int rightEdge = valueX() - 8;
        return Math.max(20, rightEdge - sliderStart(row));
    }
    private int comboWidth() { return Math.min(130, controlWidth()); }
    private int comboX() { return controlX() + controlWidth() - comboWidth(); }


    private List<DropdownOption> dropdownOptions() {
        if (openDropdown == null) return List.of();
        return switch (openDropdown) {
            case "calculatorMode" -> List.of(new DropdownOption("calculatorModeEvo", "Evo"),
                    new DropdownOption("calculatorModeEvoFast", "Evo Fast"));
            case "calculatorDiscount" -> LevelCalculator.discounts(calculatorLevel("calculatorTarget")).stream()
                    .map(percent -> new DropdownOption("calculatorDiscount" + percent,
                            percent == 0 ? tr("evoassist.calculator.noDiscount") : percent + "%")).toList();
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

        if (row.kind == Kind.LEVELS) {
            renderCalculatorInput(g, "calculatorCurrent", tr("evoassist.calculator.currentLevel"), y, mouseX, mouseY);
            renderCalculatorInput(g, "calculatorTarget", tr("evoassist.calculator.targetLevel"), y, mouseX, mouseY);
            return;
        }
        if (row.kind == Kind.INFO) {
            int textY = y + 12;
            for (var line : font.split(Component.literal(row.title), cardWidth() - 24)) {
                g.text(font, line, cardX() + 12, textY, MUTED);
                textY += font.lineHeight;
            }
            return;
        }
        if (row.kind == Kind.RESULT) {
            String shown = resultLabel(row.id);
            int color = row.id.equals("calculatorMoney") ? 0xFF55FF55 : ACCENT;
            if (narrow()) {
                g.text(font, fit(row.title, cardWidth() - 24), cardX() + 12, y + 8, MUTED);
                g.text(font, shown, cardX() + 12, y + 25, color);
            } else {
                int valueWidth = font.width(shown);
                g.text(font, fit(row.title, cardWidth() - valueWidth - 36), cardX() + 12, y + 12, MUTED);
                g.text(font, shown, cardX() + cardWidth() - valueWidth - 12, y + 12, color);
            }
            return;
        }

        int labelWidth = narrow() ? cardWidth() - 24 : controlX() - cardX() - 22;
        g.text(font, fit(row.title, labelWidth), cardX() + 12, y + 11, TEXT);
        if (!narrow()) {
            g.text(font, fit(row.hint, labelWidth), cardX() + 12, y + 27, MUTED);
        }
        int cy = controlY(y);
        int cx = controlX();
        int cw = controlWidth();

        if (row.kind == Kind.SLIDER) {
            int current = value(row.id);
            int trackX = sliderStart(row);
            int trackWidth = sliderWidth(row);
            int filled = Math.round(trackWidth * Math.clamp((current - row.min) / (float) (row.max - row.min), 0, 1));
            renderControl(g, "track:" + row.id, trackX - 4, cy + 2, trackWidth + 8, 16, mouseX, mouseY);
            surface(g, trackX, cy + 8, trackX + trackWidth, cy + 12, TRACK);
            surface(g, trackX, cy + 8, trackX + filled, cy + 12, ACCENT);
            surface(g, trackX + filled - 2, cy + 5, trackX + filled + 2, cy + 15, TEXT);
            renderControl(g, "value:" + row.id, valueX(), cy - 2, 40, 22, mouseX, mouseY);
            String shown = row.id.equals(editingId) ? editingText + "_" : Integer.toString(current);
            g.centeredText(font, fit(shown, 36), valueX() + 20, cy + 5, TEXT);
            renderReset(g, row.id, cy, mouseX, mouseY);
            return;
        }

        if (row.kind == Kind.NUMBER || row.kind == Kind.AMOUNT) {
            int boxX = cx;
            renderControl(g, "value:" + row.id, boxX, cy - 2, cw, 22, mouseX, mouseY);
            String shown = row.id.equals(editingId) ? editingText + "_"
                    : row.kind == Kind.AMOUNT ? amountLabel(row.id) : Integer.toString(value(row.id));
            g.centeredText(font, fit(shown, cw - 8), boxX + cw / 2, cy + 5, TEXT);
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

    private static boolean calculatorInput(String id) {
        return "calculatorCurrent".equals(id) || "calculatorTarget".equals(id);
    }

    private String calculatorText(String id) {
        if (id.equals(editingId)) return editingText;
        return id.equals("calculatorCurrent") ? ConfigCalculator.currentLevel : ConfigCalculator.targetLevel;
    }

    private int calculatorLevel(String id) {
        try { return Integer.parseInt(calculatorText(id)); }
        catch (NumberFormatException error) { return 0; }
    }

    private int calculatorDiscount() {
        return LevelCalculator.discounts(calculatorLevel("calculatorTarget")).contains(ConfigCalculator.discount)
                ? ConfigCalculator.discount : 0;
    }

    private static LevelCalculator.Mode calculatorMode() {
        return ConfigCalculator.mode == null ? LevelCalculator.Mode.EVO : ConfigCalculator.mode;
    }

    private String calculatorError() {
        if (calculatorText("calculatorCurrent").isEmpty() || calculatorText("calculatorTarget").isEmpty())
            return tr("evoassist.calculator.enterLevels");
        if (!LevelCalculator.validLevel(calculatorLevel("calculatorCurrent"))
                || !LevelCalculator.validLevel(calculatorLevel("calculatorTarget")))
            return tr("evoassist.calculator.levelRange");
        if (calculatorLevel("calculatorTarget") <= calculatorLevel("calculatorCurrent"))
            return tr("evoassist.calculator.targetHigher");
        return null;
    }

    private String resultLabel(String id) {
        if (calculatorError() != null) return "—";
        var result = LevelCalculator.calculate(calculatorLevel("calculatorCurrent"),
                calculatorLevel("calculatorTarget"), calculatorDiscount(), calculatorMode());
        return switch (id) {
            case "calculatorDifference" -> Integer.toString(result.levels());
            case "calculatorBlocks" -> RewardMessageParser.whole(result.blocks());
            case "calculatorMoney" -> GoalAmount.format(result.money());
            default -> "";
        };
    }

    private DropdownBounds calculatorInputBounds(String id, int rowY) {
        boolean target = id.equals("calculatorTarget");
        int x = cardX() + 12;
        int w = cardWidth() - 24;
        int y = rowY + 30;
        if (cardWidth() < 280) {
            if (target) y += 48;
        } else {
            w = (w - 12) / 2;
            if (target) x += w + 12;
        }
        return new DropdownBounds(x, y, w, 22);
    }

    private void renderCalculatorInput(GuiGraphicsExtractor g, String id, String title,
                                       int rowY, int mouseX, int mouseY) {
        var box = calculatorInputBounds(id, rowY);
        g.text(font, fit(title, box.width), box.x, box.y - 17, TEXT);
        renderControl(g, "value:" + id, box.x, box.y, box.width, box.height, mouseX, mouseY);
        String text = calculatorText(id);
        boolean focused = id.equals(editingId);
        String shown = text.isEmpty() && !focused ? "—" : text + (focused ? "_" : "");
        g.centeredText(font, shown, box.x + box.width / 2, box.y + 7,
                focused && calculatorSelectAll ? ACCENT : TEXT);
    }

    private void renderReset(GuiGraphicsExtractor g, String id, int cy, int mouseX, int mouseY) {
        renderResetButton(g, id, resetX(), cy - 2, 39, tr("evoassist.ui.reset"), mouseX, mouseY);
    }

    private void renderResetButton(GuiGraphicsExtractor g, String id, int x, int y, int width, String label,
                                   int mouseX, int mouseY) {
        boolean active = resetAvailable(id);
        renderControl(g, "reset:" + id, x, y, width, 22, mouseX, mouseY, active);
        long elapsed = System.currentTimeMillis() - resetFlashAt;
        boolean flashing = active && id.equals(resetFlashId) && elapsed >= 0 && elapsed < RESET_FLASH_DURATION_MS;
        if (flashing) {
            int alpha = (int) (220 * (RESET_FLASH_DURATION_MS - elapsed) / RESET_FLASH_DURATION_MS);
            surface(g, x, y, x + width, y + 22, (alpha << 24) | (ACCENT & 0x00FFFFFF));
        }
        if (active || isGoal(id)) {
            g.centeredText(font, label, x + width / 2, y + 7, active ? TEXT : 0xFF7D8993);
        } else {
            var lines = font.split(Component.translatable("evoassist.ui.noStatistics"), Math.max(1, width - 8));
            int textY = y + (22 - lines.size() * font.lineHeight) / 2;
            for (var line : lines) {
                g.text(font, line, x + (width - font.width(line)) / 2, textY, 0xFF7D8993);
                textY += font.lineHeight;
            }
        }
    }

    private int resetDialogWidth() { return Math.min(300, width - 24); }
    private int resetDialogX() { return (width - resetDialogWidth()) / 2; }
    private List<FormattedCharSequence> resetDescriptionLines() {
        List<String> lines = new ArrayList<>();
        if (pendingGoalChange != null) {
            lines.add(tr("evoassist.ui.currentProgress"));
            addGoalStatistics(lines, pendingGoalChange.id);
            lines.add(tr("evoassist.ui.newGoal", goalTargetLabel(pendingGoalChange.id, pendingGoalChange.target)));
            lines.add("");
            lines.add(tr("evoassist.ui.goalChangeWarning"));
        } else {
            addResetStatistics(lines, pendingResetId);
            String key = switch (pendingResetId) {
            case "resetMining" -> "evoassist.ui.resetMining.description";
            case "resetGoals" -> "evoassist.ui.resetGoals.description";
            case "resetClanGoals" -> "evoassist.ui.resetClanGoals.description";
            case "resetBossRewards" -> "evoassist.ui.resetBossRewards.description";
            case "resetBossTokens" -> "evoassist.ui.resetBossTokens.description";
            case "resetClan" -> "evoassist.ui.resetClan.description";
            case "resetKills" -> "evoassist.ui.resetKills.description";
                default -> isGoal(pendingResetId) ? "evoassist.ui.goalChangeWarning" : "";
            };
            if (!key.isEmpty()) {
                if (!lines.isEmpty()) lines.add("");
                lines.add(tr(key));
            }
        }
        return lines.isEmpty() ? List.of() : font.split(Component.literal(String.join("\n", lines)),
                Math.max(1, resetDialogWidth() - 30));
    }

    private static void addMiningGoalStatistics(List<String> lines, boolean blocks) {
        var goal = blocks ? MiningGoals.getInstance().blockGoal() : MiningGoals.getInstance().timeGoal();
        lines.add(blocks ? tr("evoassist.hud.blocks") + RewardMessageParser.whole(goal.blocks) + " / " + RewardMessageParser.whole(goal.target())
                : tr("evoassist.hud.time") + TimeUtils.asTextTime(goal.activeMillis) + " / " + TimeUtils.asTextTime(goal.target() * 60_000L));
        if (blocks) lines.add(tr("evoassist.hud.time") + TimeUtils.asTextTime(goal.activeMillis));
        else lines.add(tr("evoassist.hud.blocks") + RewardMessageParser.whole(goal.blocks));
        lines.add(tr("evoassist.hud.money") + GoalAmount.format(BigDecimal.valueOf(goal.money))
                + "; " + tr("evoassist.hud.shards") + RewardMessageParser.whole(goal.shards));
    }

    private static void addAdditionalGoalStatistics(List<String> lines, AdditionalGoals.Type type) {
        var goal = AdditionalGoals.getInstance().goal(type);
        lines.add(tr(type.title) + ": " + AdditionalGoals.format(type, goal.progress) + " / " + AdditionalGoals.format(type, goal.target));
        if (!type.clan) {
            lines.add(tr("evoassist.hud.blocks") + RewardMessageParser.whole(goal.blocks)
                    + "; " + tr("evoassist.hud.time") + TimeUtils.asTextTime(goal.activeMillis));
            lines.add(tr("evoassist.hud.money") + GoalAmount.format(goal.money)
                    + "; " + tr("evoassist.hud.shards") + RewardMessageParser.whole(goal.shards));
        }
    }

    private static void addGoalStatistics(List<String> lines, String id) {
        var type = amountType(id);
        if (type != null) addAdditionalGoalStatistics(lines, type);
        else if (id.equals("blockGoal") || id.equals("timeGoal")) addMiningGoalStatistics(lines, id.equals("blockGoal"));
    }

    private static void addResetStatistics(List<String> lines, String id) {
        switch (id) {
            case "resetKills" -> lines.add(tr("evoassist.hud.kills")
                    + RewardMessageParser.whole(EvoAssistClient.rewardStatistics.current().kills));
            case "resetMining" -> {
                var counter = BlockProfitPerHour.getInstance();
                if (counter == null) return;
                lines.add(tr("evoassist.hud.blocks") + RewardMessageParser.whole(counter.totalBrokenBlocks));
                lines.add(tr("evoassist.hud.time") + TimeUtils.asTextTime(counter.uptime));
                lines.add(tr("evoassist.hud.money") + GoalAmount.format(BigDecimal.valueOf(counter.totalMoney)));
                lines.add(tr("evoassist.hud.shards") + RewardMessageParser.whole(counter.totalShards));
            }
            case "resetBossRewards", "resetBossTokens", "resetClan" -> {
                var totals = EvoAssistClient.rewardStatistics.current();
                if (id.equals("resetBossRewards")) {
                    lines.add(tr("evoassist.hud.money") + GoalAmount.format(totals.money));
                    lines.add(tr("evoassist.hud.shards") + RewardMessageParser.whole(totals.shards));
                } else if (id.equals("resetBossTokens")) {
                    lines.add(tr("evoassist.hud.ordinaryTokens") + RewardMessageParser.whole(totals.tokens));
                    lines.add(tr("evoassist.hud.infernalTokens") + RewardMessageParser.whole(totals.infernalTokens));
                    lines.add(tr("evoassist.hud.endTokens") + RewardMessageParser.whole(totals.endTokens));
                } else {
                    lines.add(tr("evoassist.hud.clanPoints") + RewardMessageParser.whole(totals.clanPoints));
                    lines.add(tr("evoassist.hud.clanGold") + RewardMessageParser.whole(totals.clanGold));
                    lines.add(tr("evoassist.hud.clanExperience") + RewardMessageParser.whole(totals.clanExperience));
                }
            }
            case "resetGoals", "resetClanGoals" -> {
                boolean clan = id.equals("resetClanGoals");
                if (!clan) {
                    if (MiningGoals.getInstance().blockGoal().hasProgress()) addMiningGoalStatistics(lines, true);
                    if (MiningGoals.getInstance().timeGoal().hasProgress()) addMiningGoalStatistics(lines, false);
                }
                for (var type : AdditionalGoals.Type.values()) {
                    if (type.clan == clan && AdditionalGoals.getInstance().goal(type).hasProgress())
                        addAdditionalGoalStatistics(lines, type);
                }
            }
            default -> {
                if (isGoal(id)) {
                    addGoalStatistics(lines, id);
                    lines.add(tr("evoassist.ui.newGoal", goalTargetLabel(id, "0")));
                }
            }
        }
        if (!lines.isEmpty()) lines.addFirst(tr("evoassist.ui.willReset"));
    }

    private int confirmationBodyHeight() {
        return Math.min(confirmationLines.size() * font.lineHeight, Math.max(font.lineHeight, height - 112));
    }
    private int confirmationMaxScroll() {
        return Math.max(0, confirmationLines.size() * font.lineHeight - confirmationBodyHeight());
    }
    private int resetDialogHeight() {
        return confirmationLines.isEmpty() ? 92 : 96 + confirmationBodyHeight();
    }
    private int resetDialogY() { return (height - resetDialogHeight()) / 2; }
    private int resetDialogButtonsY() { return resetDialogY() + resetDialogHeight() - 34; }
    private int resetDialogButtonWidth() { return (resetDialogWidth() - 30) / 2; }

    private void renderResetConfirmation(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        confirmationLines = resetDescriptionLines();
        g.nextStratum();
        g.fill(0, 0, width, height, 0xAA000000);
        int x = resetDialogX();
        int y = resetDialogY();
        int w = resetDialogWidth();
        int h = resetDialogHeight();
        g.fill(x, y, x + w, y + h, 0xEE35404B);
        g.fill(x, y, x + w, y + 1, 0xFF91A2B1);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF91A2B1);
        g.fill(x, y, x + 1, y + h, 0xFF91A2B1);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF91A2B1);
        g.centeredText(font, tr(pendingGoalChange != null ? "evoassist.ui.confirmGoalChange" : "evoassist.ui.confirmReset"), width / 2, y + 14, TEXT);
        String subjectId = pendingGoalChange != null ? pendingGoalChange.id : pendingResetId;
        String target = switch (subjectId) {
            case "resetMining" -> tr("evoassist.ui.resetMining.subject");
            case "resetGoals" -> tr("evoassist.ui.resetGoals.subject");
            case "resetClanGoals" -> tr("evoassist.ui.resetClanGoals.subject");
            case "resetBossRewards" -> tr("evoassist.ui.resetBossRewards.subject");
            case "resetBossTokens" -> tr("evoassist.ui.resetBossTokens.subject");
            case "resetClan" -> tr("evoassist.ui.resetClan.subject");
            case "resetKills" -> tr("evoassist.page.kills.title");
            default -> rows().stream().filter(row -> row.id.equals(subjectId))
                    .map(row -> row.title).findFirst().orElse(tr("evoassist.ui.setting"));
        };
        g.centeredText(font, fit(target, w - 20), width / 2, y + 33, MUTED);
        confirmationScroll = Math.clamp(confirmationScroll, 0, confirmationMaxScroll());
        int textY = y + 50 - confirmationScroll;
        g.enableScissor(x + 12, y + 50, x + w - 12, y + 50 + confirmationBodyHeight());
        for (var line : confirmationLines) {
            g.text(font, line, x + (w - font.width(line)) / 2, textY, MUTED);
            textY += font.lineHeight;
        }
        g.disableScissor();
        if (confirmationMaxScroll() > 0) {
            int bodyHeight = confirmationBodyHeight();
            int thumbHeight = Math.max(8, bodyHeight * bodyHeight / (bodyHeight + confirmationMaxScroll()));
            int thumbY = y + 50 + (bodyHeight - thumbHeight) * confirmationScroll / confirmationMaxScroll();
            g.fill(x + w - 8, y + 50, x + w - 6, y + 50 + bodyHeight, TRACK);
            g.fill(x + w - 8, thumbY, x + w - 6, thumbY + thumbHeight, MUTED);
        }
        int buttonWidth = resetDialogButtonWidth();
        int buttonY = resetDialogButtonsY();
        renderDialogButton(g, "cancelReset", tr("evoassist.ui.cancel"), x + 10, buttonY, buttonWidth, mouseX, mouseY, true);
        renderDialogButton(g, "confirmReset", tr(pendingGoalChange != null ? "evoassist.ui.setGoal" : "evoassist.ui.resetAction"),
                x + 20 + buttonWidth, buttonY, buttonWidth, mouseX, mouseY,
                pendingGoalChange != null || resetAvailable(pendingResetId));
    }

    private void renderDialogButton(GuiGraphicsExtractor g, String id, String label,
                                    int x, int y, int w, int mouseX, int mouseY, boolean active) {
        float amount = hoverAmount(id, active && inside(mouseX, mouseY, x, y, w, 24));
        g.fill(x, y, x + w, y + 24, active ? mixColor(0xFF405061, 0xFF5B7183, amount) : 0xFF303A43);
        int border = active ? mixColor(0xFF91A2B1, ACCENT, amount) : 0xFF596671;
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + 23, x + w, y + 24, border);
        g.fill(x, y, x + 1, y + 24, border);
        g.fill(x + w - 1, y, x + w, y + 24, border);
        g.centeredText(font, label, x + w / 2, y + 8, active ? TEXT : 0xFF7D8993);
    }

    private static boolean resetAvailable(String id) {
        var type = amountType(id);
        if (type != null) {
            try {
                return GoalAmount.parse(AdditionalGoals.configuredTarget(type), type.whole()).signum() != 0;
            } catch (IllegalArgumentException error) {
                return true; // Allow clearing an invalid value entered through another config editor.
            }
        }
        return switch (id) {
            case "blockGoal" -> ConfigMining.blockGoalTarget != 0;
            case "timeGoal" -> ConfigMining.timeGoalMinutes != 0;
            case "resetMining" -> {
                var counter = BlockProfitPerHour.getInstance();
                yield counter != null && counter.hasStatistics();
            }
            case "resetGoals" -> MiningGoals.getInstance().hasProgress() || AdditionalGoals.getInstance().hasProgress(false);
            case "resetClanGoals" -> AdditionalGoals.getInstance().hasProgress(true);
            case "resetBossRewards" -> EvoAssistClient.rewardStatistics != null && EvoAssistClient.rewardStatistics.hasBossRewardStatistics();
            case "resetBossTokens" -> EvoAssistClient.rewardStatistics != null && EvoAssistClient.rewardStatistics.hasBossTokenStatistics();
            case "resetClan" -> EvoAssistClient.rewardStatistics != null && EvoAssistClient.rewardStatistics.hasClanStatistics();
            case "resetKills" -> EvoAssistClient.rewardStatistics != null && EvoAssistClient.rewardStatistics.hasKillStatistics();
            default -> true;
        };
    }

    private void requestReset(String id) {
        if (!resetAvailable(id)) return;
        pendingResetId = id;
        confirmationScroll = 0;
        confirmationLines = resetDescriptionLines();
        openDropdown = null;
    }

    private void confirmReset() {
        if (pendingGoalChange != null) {
            GoalChange change = pendingGoalChange;
            pendingGoalChange = null;
            applyGoalChange(change.id, change.target);
            save();
            return;
        }
        String id = pendingResetId;
        pendingResetId = null;
        if (id == null || !resetAvailable(id)) return;
        switch (id) {
            case "resetMining" -> resetMining();
            case "resetGoals" -> resetGoals();
            case "resetClanGoals" -> {
                AdditionalGoals.getInstance().reset(true);
                feedback = tr("evoassist.ui.clanGoalsReset");
                feedbackUntil = System.currentTimeMillis() + 2500;
                startResetFlash(id);
            }
            case "resetBossRewards", "resetBossTokens", "resetClan", "resetKills" -> resetRewards(id);
            default -> resetValue(id);
        }
    }

    private void startResetFlash(String id) {
        resetFlashId = id;
        resetFlashAt = System.currentTimeMillis();
    }


    private static boolean enabled(String id) {
        return switch (id) {
            case "clanGoalNotifications" -> ConfigClan.goalNotifications;
            case "goalNotifications" -> ConfigMining.goalNotifications;
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

    private String choice(String id) {
        return switch (id) {
            case "calculatorMode" -> calculatorMode() == LevelCalculator.Mode.EVO_FAST ? "Evo Fast" : "Evo";
            case "calculatorDiscount" -> calculatorDiscount() == 0 ? tr("evoassist.calculator.noDiscount") : calculatorDiscount() + "%";
            case "chatOrientation" -> ConfigChat.chatTabsOrientation.toString();
            default -> "";
        };
    }

    private static KeyMapping bindingFor(String id) {
        if (!id.equals("menuKey")) throw new IllegalArgumentException("Unknown key binding: " + id);
        return EvoAssistClient.menuKey();
    }

    private static int value(String id) {
        return switch (id) {
            case "menuOpacity" -> ConfigVisual.menuOpacity;
            case "goalNoticeDuration" -> ConfigVisual.goalNoticeDurationSeconds;
            case "blockGoal" -> ConfigMining.blockGoalTarget;
            case "timeGoal" -> ConfigMining.timeGoalMinutes;
            default -> 0;
        };
    }

    private static void setValue(String id, int newValue) {
        switch (id) {
            case "menuOpacity" -> ConfigVisual.menuOpacity = Math.clamp(newValue, 10, 100);
            case "goalNoticeDuration" -> ConfigVisual.goalNoticeDurationSeconds = Math.clamp(newValue, 1, 60);
            case "blockGoal" -> {
                ConfigMining.blockGoalTarget = newValue;
                MiningGoals.getInstance().syncTargets();
            }
            case "timeGoal" -> {
                ConfigMining.timeGoalMinutes = Math.clamp(newValue, 0, 9_999);
                MiningGoals.getInstance().syncTargets();
            }
        }
    }

    private void resetValue(String id) {
        if (amountType(id) != null) setAmount(id, "0");
        switch (id) {
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
            case "clanGoalNotifications" -> ConfigClan.goalNotifications = !ConfigClan.goalNotifications;
            case "goalNotifications" -> ConfigMining.goalNotifications = !ConfigMining.goalNotifications;
        }
        save();
    }

    private void selectOption(String id) {
        if (id.equals("calculatorModeEvo") || id.equals("calculatorModeEvoFast")) {
            ConfigCalculator.mode = id.equals("calculatorModeEvoFast") ? LevelCalculator.Mode.EVO_FAST : LevelCalculator.Mode.EVO;
            openDropdown = null;
            save();
            return;
        }
        if (id.startsWith("calculatorDiscount")) {
            int discount = Integer.parseInt(id.substring("calculatorDiscount".length()));
            if (LevelCalculator.discounts(calculatorLevel("calculatorTarget")).contains(discount))
                ConfigCalculator.discount = discount;
            openDropdown = null;
            save();
            return;
        }
        switch (id) {
            case "chatHorizontal" -> ConfigChat.chatTabsOrientation = ConfigChat.Orientation.HORIZONTAL;
            case "chatVertical" -> ConfigChat.chatTabsOrientation = ConfigChat.Orientation.VERTICAL;
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
        if (!EvoAssistClient.rewardStatistics.hasServer()) {
            feedback = tr("evoassist.ui.connectFirst");
        } else {
            switch (id) {
                case "resetBossRewards" -> EvoAssistClient.rewardStatistics.resetBossRewards();
                case "resetBossTokens" -> EvoAssistClient.rewardStatistics.resetBossTokens();
                case "resetClan" -> EvoAssistClient.rewardStatistics.resetClan();
                case "resetKills" -> EvoAssistClient.rewardStatistics.resetKills();
                default -> throw new IllegalArgumentException(id);
            }
            feedback = tr("evoassist.ui.statisticsReset");
            startResetFlash(id);
        }
        feedbackUntil = System.currentTimeMillis() + 2500;
    }

    private void updateSlider(Row row, double mouseX) {
        double fraction = Math.clamp((mouseX - sliderStart(row)) / sliderWidth(row), 0, 1);
        setValue(row.id, row.min + (int) Math.round(fraction * (row.max - row.min)));
        editingId = null;
    }

    private void startEditing(String id) {
        editingId = id;
        editingText = calculatorInput(id) ? id.equals("calculatorCurrent")
                ? ConfigCalculator.currentLevel : ConfigCalculator.targetLevel : "";
        calculatorSelectAll = calculatorInput(id);
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
            assert type != null;
            return AdditionalGoals.format(type, GoalAmount.parse(
                    AdditionalGoals.configuredTarget(type), type.whole()).min(type.maximum()));
        } catch (IllegalArgumentException error) { return "0"; }
    }

    private static void setAmount(String id, String input) {
        var type = amountType(id);
        assert type != null;
        String value = AdditionalGoals.parseTarget(type, input).toPlainString();
        AdditionalGoals.setConfiguredTarget(type, value);
        AdditionalGoals.getInstance().syncTargets();
    }

    private static boolean isGoal(String id) {
        return id.equals("blockGoal") || id.equals("timeGoal") || amountType(id) != null;
    }

    private static String goalTargetLabel(String id, String target) {
        var type = amountType(id);
        if (type != null) return AdditionalGoals.format(type, new BigDecimal(target));
        long value = Long.parseLong(target);
        return id.equals("timeGoal") ? TimeUtils.asTextTime(value * 60_000L) : RewardMessageParser.whole(value);
    }

    private static boolean wouldResetGoalProgress(String id, String target) {
        var type = amountType(id);
        if (type != null) return AdditionalGoals.getInstance().goal(type).wouldResetProgress(new BigDecimal(target));
        var goal = id.equals("blockGoal") ? MiningGoals.getInstance().blockGoal() : MiningGoals.getInstance().timeGoal();
        return goal.wouldResetProgress(Integer.parseInt(target));
    }

    private static void applyGoalChange(String id, String target) {
        if (amountType(id) != null) setAmount(id, target);
        else setValue(id, Integer.parseInt(target));
    }

    private boolean hasConfirmation() { return pendingResetId != null || pendingGoalChange != null; }

    private void cancelConfirmation() {
        pendingResetId = null;
        pendingGoalChange = null;
        confirmationScroll = 0;
        confirmationLines = List.of();
    }

    private boolean commitEditing() {
        if (editingId == null) return !hasConfirmation();
        String id = editingId;
        if (calculatorInput(id)) {
            if (id.equals("calculatorCurrent")) ConfigCalculator.currentLevel = editingText;
            else ConfigCalculator.targetLevel = editingText;
            ConfigCalculator.discount = calculatorDiscount();
            editingId = null;
            calculatorSelectAll = false;
            save();
            return true;
        }
        try {
            if (!editingText.isEmpty()) {
                String target;
                if (amountType(id) != null) target = AdditionalGoals.parseTarget(amountType(id), editingText).toPlainString();
                else {
                    int typed = Integer.parseInt(editingText);
                    Row row = rows().stream().filter(item -> item.id.equals(id)).findFirst().orElseThrow();
                    target = Integer.toString(Math.clamp(typed, row.min, row.max));
                }
                if (isGoal(id) && wouldResetGoalProgress(id, target)) {
                    pendingGoalChange = new GoalChange(id, target);
                    confirmationScroll = 0;
                    confirmationLines = resetDescriptionLines();
                    openDropdown = null;
                } else applyGoalChange(id, target);
            }
        } catch (IllegalArgumentException error) {
            feedback = amountType(id) == null ? tr("evoassist.ui.integerRequired") : error.getMessage();
            feedbackUntil = System.currentTimeMillis() + 4000;
        }
        editingId = null;
        save();
        return !hasConfirmation();
    }

    private void save() {
        EvoAssistClient.configurator.saveConfig(Config.class);
    }

    private boolean prepareControlClick() {
        boolean ready = commitEditing();
        capturingKeyId = null;
        openDropdown = null;
        return ready;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (hasConfirmation()) {
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                int x = resetDialogX();
                int y = resetDialogButtonsY();
                int buttonWidth = resetDialogButtonWidth();
                if (inside(event.x(), event.y(), x + 10, y, buttonWidth, 24)) {
                    cancelConfirmation();
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
                if (!commitEditing()) return true;
                capturingKeyId = null;
                selectOption(options.get(optionIndex).id);
            }
            return true;
        }

        int navY = top() + 48;
        for (Page item : Page.values()) {
            if (inside(mx, my, left() + 7, navY, sidebarWidth() - 14, navHeight())) {
                if (!prepareControlClick()) return true;
                page = item;
                scroll = 0;
                return true;
            }
            navY += navStep();
        }
        if (inside(mx, my, right() - 69, bottom() - 31, 60, 24)) {
            if (!prepareControlClick()) return true;
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
                    case SECTION, INFO, RESULT -> { }
                    case LEVELS -> {
                        for (String id : List.of("calculatorCurrent", "calculatorTarget")) {
                            var box = calculatorInputBounds(id, y);
                            if (inside(mx, my, box.x, box.y, box.width, box.height)) {
                                if (!prepareControlClick()) return true;
                                startEditing(id);
                                break;
                            }
                        }
                    }
                    case TOGGLE -> {
                        if (inside(mx, my, cx + cw - 32, cy + 2, 32, 14)) {
                            if (!prepareControlClick()) return true;
                            toggle(row.id);
                        }
                    }
                    case CHOICE, MULTI -> {
                        if (inside(mx, my, comboX(), cy - 2, comboWidth(), 22)) {
                            String previousDropdown = openDropdown;
                            if (!prepareControlClick()) return true;
                            openDropdown = row.id.equals(previousDropdown) ? null : row.id;
                        }
                    }
                    case KEY -> {
                        if (inside(mx, my, cx, cy - 2, cw, 22)) {
                            if (!prepareControlClick()) return true;
                            capturingKeyId = row.id;
                        }
                    }
                    case ACTION -> {
                        if (resetAvailable(row.id) && inside(mx, my, cx, cy - 2, cw, 22)) {
                            if (!prepareControlClick()) return true;
                            requestReset(row.id);
                        }
                    }
                    case NUMBER, AMOUNT -> {
                        if (inside(mx, my, cx, cy - 2, cw, 22)) {
                            if (!prepareControlClick()) return true;
                            startEditing(row.id);
                        }
                    }
                    case SLIDER -> {
                        if (inside(mx, my, resetX(), cy - 2, 39, 22)) {
                            if (!prepareControlClick()) return true;
                            requestReset(row.id);
                        } else if (inside(mx, my, valueX(), cy - 2, 40, 22)) {
                            if (!prepareControlClick()) return true;
                            startEditing(row.id);
                        } else if (inside(mx, my, sliderStart(row) - 4, cy + 2, sliderWidth(row) + 8, 16)) {
                            if (!prepareControlClick()) return true;
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
    public boolean mouseDragged(@NonNull MouseButtonEvent event, double dx, double dy) {
        if (hasConfirmation()) return true;
        if (draggingSlider != null) {
            updateSlider(draggingSlider, event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
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
        if (hasConfirmation()) {
            confirmationScroll = Math.clamp(confirmationScroll - (int) Math.round(yAmount * font.lineHeight * 3),
                    0, confirmationMaxScroll());
            return true;
        }
        if (inside(mouseX, mouseY, contentX(), contentTop(), contentWidth(), contentBottom() - contentTop())) {
            openDropdown = null;
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(yAmount * 28)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, xAmount, yAmount);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (hasConfirmation()) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) cancelConfirmation();
            else if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) confirmReset();
            else if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN)
                confirmationScroll = Math.clamp(confirmationScroll + (event.key() == GLFW.GLFW_KEY_DOWN ? 1 : -1) * font.lineHeight * 3,
                        0, confirmationMaxScroll());
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
            if (calculatorInput(editingId)) {
                boolean control = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
                if (control && event.key() == GLFW.GLFW_KEY_A) {
                    calculatorSelectAll = true;
                    return true;
                }
                if (control && event.key() == GLFW.GLFW_KEY_V) {
                    String pasted = minecraft.keyboardHandler.getClipboard().strip();
                    String next = calculatorSelectAll ? pasted : editingText + pasted;
                    if (next.matches("[0-9]{0,3}")) {
                        editingText = next;
                        calculatorSelectAll = false;
                    }
                    return true;
                }
                if (event.key() == GLFW.GLFW_KEY_DELETE || event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                    if (calculatorSelectAll || event.key() == GLFW.GLFW_KEY_DELETE) editingText = "";
                    else if (!editingText.isEmpty()) editingText = editingText.substring(0, editingText.length() - 1);
                    calculatorSelectAll = false;
                    return true;
                }
            }
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
    public boolean charTyped(@NonNull CharacterEvent event) {
        if (hasConfirmation()) return true;
        if (editingId != null) {
            int cp = event.codepoint();
            if (calculatorInput(editingId)) {
                if (cp >= '0' && cp <= '9') {
                    if (calculatorSelectAll) editingText = "";
                    calculatorSelectAll = false;
                    if (editingText.length() < 3) editingText += (char) cp;
                }
                return true;
            }
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
        if (!commitEditing()) return;
        save();
        minecraft.gui.setScreen(parent);
    }
}
