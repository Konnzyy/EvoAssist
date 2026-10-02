package com.konzy.evo_assist.client.ui;

import static com.konzy.evo_assist.client.util.Texts.tr;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.Config;
import com.konzy.evo_assist.client.config.Hidden.HudConfig;
import com.konzy.evo_assist.client.ui.widgets.WWidget;
import com.konzy.evo_assist.client.ui.widgets.WiBlockProfitPH;
import com.konzy.evo_assist.client.ui.widgets.WiMiningGoal;
import com.konzy.evo_assist.client.ui.widgets.WiGoalNotice;
import com.konzy.evo_assist.client.ui.widgets.WiRewards;
import com.konzy.evo_assist.client.ui.widgets.WiAdditionalGoal;
import com.konzy.evo_assist.client.ui.widgets.WiChatTabs;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;


public class WidgetScreen extends Screen {
    public enum Focus { ALL, BOSSES, CLAN }
    private final Screen parent;
    private final Focus focus;
    private WiBlockProfitPH wiBlockProfitPH;
    private WiMiningGoal wiBlockGoal;
    private WiMiningGoal wiTimeGoal;
    private WiGoalNotice wiGoalNotice;
    private WiRewards wiBosses;
    private WiRewards wiClan;
    private WiChatTabs wiChatTabs;
    private final List<WiAdditionalGoal> additionalGoals = new ArrayList<>();

    public WidgetScreen() {
        this(null);
    }

    public WidgetScreen(Screen parent) {
        this(parent, Focus.ALL);
    }

    public WidgetScreen(Screen parent, Focus focus) {
        super(Component.translatable("evoassist.editor.title"));
        this.parent = parent;
        this.focus = focus;
    }

    @Override
    protected void init() {
        super.init();
        initScreenWidgets();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double xAmount, double yAmount) {
        for (GuiEventListener element : this.children()) {
            if (element instanceof AbstractWidget widget && widget.isMouseOver(mouseX, mouseY)) {
                if(widget instanceof WWidget wWidget) {
                    wWidget.resize(yAmount);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, xAmount, yAmount);
    }

    private void initScreenWidgets() {
        additionalGoals.clear();
        if (focus == Focus.ALL) {
        wiBlockProfitPH = new WiBlockProfitPH(
                HudConfig.WidgetBphX,
                HudConfig.WidgetBphY,
                230, 31, this
        );

        this.addRenderableWidget(wiBlockProfitPH);
        wiBlockGoal = new WiMiningGoal(true, HudConfig.BlockGoalX, HudConfig.BlockGoalY, this);
        wiTimeGoal = new WiMiningGoal(false, HudConfig.TimeGoalX, HudConfig.TimeGoalY, this);
        wiGoalNotice = new WiGoalNotice(HudConfig.GoalNoticeX, HudConfig.GoalNoticeY, this);
        this.addRenderableWidget(wiBlockGoal);
        this.addRenderableWidget(wiTimeGoal);
        this.addRenderableWidget(wiGoalNotice);
        wiChatTabs = new WiChatTabs(this, null);
        this.addRenderableWidget(wiChatTabs);
        for (AdditionalGoals.Type type : AdditionalGoals.Type.values()) {
            WiAdditionalGoal widget = new WiAdditionalGoal(type, this);
            additionalGoals.add(widget);
            this.addRenderableWidget(widget);
        }
        }
        if (focus == Focus.ALL || focus == Focus.BOSSES) {
            wiBosses = new WiRewards(false, HudConfig.BossX, HudConfig.BossY, this);
            this.addRenderableWidget(wiBosses);
        }
        if (focus == Focus.ALL || focus == Focus.CLAN) {
            wiClan = new WiRewards(true, HudConfig.ClanX, HudConfig.ClanY, this);
            this.addRenderableWidget(wiClan);
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Keep the world and other mods' HUD visible while arranging our widgets.
        if (minecraft.level == null) super.extractBackground(graphics, mouseX, mouseY, delta);
        else minecraft.gui.hud.extractDeferredSubtitles();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(minecraft.font, tr("evoassist.editor.help"),
                width / 2, height - 14, 0xFFFFFFFF);
    }
    @Override
    public void onClose() {
        if(wiBlockProfitPH != null) {
            HudConfig.WidgetBphX = wiBlockProfitPH.getX();
            HudConfig.WidgetBphY = wiBlockProfitPH.getY();
        }
        if (wiBlockGoal != null) {
            HudConfig.BlockGoalX = wiBlockGoal.getX();
            HudConfig.BlockGoalY = wiBlockGoal.getY();
        }
        if (wiTimeGoal != null) {
            HudConfig.TimeGoalX = wiTimeGoal.getX();
            HudConfig.TimeGoalY = wiTimeGoal.getY();
        }
        if (wiGoalNotice != null) {
            wiGoalNotice.savePosition();
        }
        if (wiBosses != null) wiBosses.savePosition();
        if (wiClan != null) wiClan.savePosition();
        if (wiChatTabs != null) wiChatTabs.savePosition();
        additionalGoals.forEach(WiAdditionalGoal::savePosition);

        boolean configurator = EvoAssistClient.configurator.saveConfig(Config.class);

        EvoAssistClient.evoClient.initHudWidgets();

        if (parent != null) {
            minecraft.gui.setScreen(parent);
        } else {
            super.onClose();
        }
    }




}
