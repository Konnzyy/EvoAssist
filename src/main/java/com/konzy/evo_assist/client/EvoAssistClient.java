package com.konzy.evo_assist.client;

import static com.konzy.evo_assist.client.util.Texts.tr;
import com.konzy.evo_assist.client.chat.ChatTabManager;
import com.konzy.evo_assist.client.config.Config;
import com.konzy.evo_assist.client.config.ConfigAutoclicker;
import com.konzy.evo_assist.client.config.Hidden.HudConfig;
import com.konzy.evo_assist.client.event.ChatGameEvent;
import com.konzy.evo_assist.client.features.autoclicker.Clicker;
import com.konzy.evo_assist.client.features.mine.blockPH.BlockProfitPerHour;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import com.konzy.evo_assist.client.features.rewards.RewardStatistics;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import com.konzy.evo_assist.client.ui.widgets.WiAdditionalGoal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import com.konzy.evo_assist.client.ui.EvoConfigScreen;
import com.konzy.evo_assist.client.ui.WidgetScreen;
import com.konzy.evo_assist.client.ui.widgets.WiBlockProfitPH;
import com.konzy.evo_assist.client.ui.widgets.WiMiningGoal;
import com.konzy.evo_assist.client.ui.widgets.WiGoalNotice;
import com.konzy.evo_assist.client.ui.widgets.WWidget;
import com.konzy.evo_assist.client.ui.widgets.WiRewards;
import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;


public class EvoAssistClient implements ClientModInitializer {

    public static final Logger logger = LoggerFactory.getLogger("EvoAssist");
    public static final String MODID = "evoassist";
    public static final Configurator configurator = new Configurator(MODID);

    private static final Identifier WIDGET_LAYER = Identifier.fromNamespaceAndPath(MODID, "hud-widget-layer");

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MODID, "controls"));
    private static final KeyMapping ClickerBind = new KeyMapping("key.evoassist.autoclicker", GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    private static final KeyMapping ClickerModeBind = new KeyMapping("key.evoassist.toggle_mode", GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
    private static final KeyMapping ConfigBind = new KeyMapping("key.evoassist.open_menu", GLFW.GLFW_KEY_INSERT, CATEGORY);

    public static Minecraft instance;
    public static EvoAssistClient evoClient;

    private WWidget wBlockProfitPH;
    private WWidget wBlockGoal;
    private WWidget wTimeGoal;
    private WWidget wGoalNotice;
    private WWidget wBosses;
    private WWidget wClan;
    private final EnumMap<AdditionalGoals.Type, WiAdditionalGoal> additionalGoalWidgets = new EnumMap<>(AdditionalGoals.Type.class);
    private final List<WWidget> hudWidgets = new ArrayList<>();
    public static RewardStatistics rewardStatistics;
    private boolean widgetsInitialized = false;
    private boolean chatFilterInstalled = false;
    public static BlockProfitPerHour eventBlockProfitPerHour;
    public static ChatGameEvent eventChatGame;
    public static KeyMapping clickerKey() { return ClickerBind; }
    public static KeyMapping clickerModeKey() { return ClickerModeBind; }
    public static KeyMapping menuKey() { return ConfigBind; }
    @Override
    public void onInitializeClient() {
        com.konzy.evo_assist.client.util.Texts.useTranslator(net.minecraft.client.resources.language.I18n::get);
        logger.info("EvoAssist initialized");

        instance = Minecraft.getInstance();
        evoClient = this;

        configurator.register(Config.class);
        if (AdditionalGoals.normalizeConfiguredTargets()) configurator.saveConfig(Config.class);
        rewardStatistics = new RewardStatistics(
                FabricLoader.getInstance().getConfigDir().resolve("evoassist_rewards.properties"),
                error -> logger.warn("Could not update boss/clan reward statistics", error),
                reward -> AdditionalGoals.getInstance().reward(reward));
        MiningGoals.getInstance();
        AdditionalGoals.getInstance();
        Clicker.stop();
        ChatTabManager.getInstance();

        //KeyBinds

        KeyMappingHelper.registerKeyMapping(ConfigBind);
        KeyMappingHelper.registerKeyMapping(ClickerBind);
        KeyMappingHelper.registerKeyMapping(ClickerModeBind);



        // Event register

        eventBlockProfitPerHour = new BlockProfitPerHour();
        eventChatGame = new ChatGameEvent();
        ClientPlayerBlockBreakEvents.AFTER.register(eventBlockProfitPerHour);
        ClientReceiveMessageEvents.GAME.register(eventChatGame);
        ClientPlayConnectionEvents.JOIN.register((_, _, client) -> {
            eventBlockProfitPerHour.reset();
            var server = client.getCurrentServer();
            if (server != null) {
                rewardStatistics.connect(server.ip);
                AdditionalGoals.getInstance().connect(server.ip);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> {
            rewardStatistics.disconnect();
            AdditionalGoals.getInstance().disconnect();
            MiningGoals.getInstance().expirePendingPrices();
            MiningGoals.getInstance().save();
            eventBlockProfitPerHour.reset();
        });

        // Commands

        ClientCommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess) -> {
            commandDispatcher.register(ClientCommands.literal("evoassist").executes(commandContext -> {
                instance.schedule(() -> instance.gui.setScreen(new EvoConfigScreen(instance.gui.screen())));
                return 1;
            }));
            commandDispatcher.register(ClientCommands.literal("ea").executes(commandContext -> {
                instance.schedule(() -> instance.gui.setScreen(new EvoConfigScreen(instance.gui.screen())));
                return 1;
            }));
            commandDispatcher.register(ClientCommands.literal("evoassistwidgets").executes(commandContext -> {
                instance.schedule(() -> instance.gui.setScreen(new WidgetScreen()));
                return 1;
            }));
            commandDispatcher.register(ClientCommands.literal("eaw").executes(commandContext -> {
                instance.schedule(() -> instance.gui.setScreen(new WidgetScreen()));
                return 1;
            }));
        }));

        AtomicLong latestSecond = new AtomicLong(System.currentTimeMillis());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!chatFilterInstalled) {
                client.gui.hud.getChat().setVisibleMessageFilter(message ->
                        !com.konzy.evo_assist.client.config.ConfigChat.chatTabsToggle
                                || ChatTabManager.getInstance().shouldDisplayMessage(message.content()));
                chatFilterInstalled = true;
            }
            if(System.currentTimeMillis() - latestSecond.get() >= 1000) {
                eventBlockProfitPerHour.second();

                latestSecond.set(System.currentTimeMillis());
            }

            if(!widgetsInitialized && client.player != null) {
                initHudWidgets();
                widgetsInitialized = true;
            }

            while (ConfigBind.consumeClick()) {
                instance.gui.setScreen(new EvoConfigScreen(instance.gui.screen()));
            }

            while (ClickerBind.consumeClick()) {
                if (ConfigAutoclicker.autoclickerEnabled && client.level != null
                        && client.player != null && client.player.isAlive()
                        && client.gui.screen() == null) {
                    ConfigAutoclicker.autoclickerToggle = !ConfigAutoclicker.autoclickerToggle;
                }
            }
            while (ClickerModeBind.consumeClick()) {
                if (client.level != null && client.player != null && client.player.isAlive()
                        && client.gui.screen() == null) {
                    Clicker.stop();
                    ConfigAutoclicker.autoclickerActivation =
                            ConfigAutoclicker.autoclickerActivation == ConfigAutoclicker.ENUMAutoClickerActivation.SWITCH
                                    ? ConfigAutoclicker.ENUMAutoClickerActivation.HOLD
                                    : ConfigAutoclicker.ENUMAutoClickerActivation.SWITCH;
                    configurator.saveConfig(Config.class);
                    client.gui.hud.setOverlayMessage(Component.literal(
                            tr("evoassist.clicker.modeNotice", ConfigAutoclicker.autoclickerActivation)), false);
                }
            }
            Clicker.tick(client);
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, WIDGET_LAYER, (context, tickCounter) -> {
            float tickDelta = tickCounter.getGameTimeDeltaPartialTick(false);
            for (WWidget widget : hudWidgets) {
                widget.extractRenderState(context, 0, 0, tickDelta);
            }
        });

    }

    public void initHudWidgets() {
        if(wBlockProfitPH == null) {
            wBlockProfitPH = new WiBlockProfitPH(HudConfig.WidgetBphX, HudConfig.WidgetBphY,
                    230, 31, null);
        } else {
            wBlockProfitPH.setX(HudConfig.WidgetBphX);
            wBlockProfitPH.setY(HudConfig.WidgetBphY);
            wBlockProfitPH.applyPos();
        }
        wBlockProfitPH.setScale(HudConfig.WidgetBphScale);

        if (wBlockGoal == null) wBlockGoal = new WiMiningGoal(true, HudConfig.BlockGoalX, HudConfig.BlockGoalY, null);
        else {
            wBlockGoal.setX(HudConfig.BlockGoalX);
            wBlockGoal.setY(HudConfig.BlockGoalY);
            wBlockGoal.applyPos();
        }
        wBlockGoal.setScale(HudConfig.BlockGoalScale);

        if (wTimeGoal == null) wTimeGoal = new WiMiningGoal(false, HudConfig.TimeGoalX, HudConfig.TimeGoalY, null);
        else {
            wTimeGoal.setX(HudConfig.TimeGoalX);
            wTimeGoal.setY(HudConfig.TimeGoalY);
            wTimeGoal.applyPos();
        }
        wTimeGoal.setScale(HudConfig.TimeGoalScale);

        if (wGoalNotice == null) wGoalNotice = new WiGoalNotice(HudConfig.GoalNoticeX, HudConfig.GoalNoticeY, null);
        else {
            wGoalNotice.setX(HudConfig.GoalNoticeX);
            wGoalNotice.setY(HudConfig.GoalNoticeY);
            wGoalNotice.applyPos();
        }
        wGoalNotice.setScale(HudConfig.GoalNoticeScale);
        if (wBosses == null) wBosses = new WiRewards(false, HudConfig.BossX, HudConfig.BossY, null);
        else {
            wBosses.setX(HudConfig.BossX);
            wBosses.setY(HudConfig.BossY);
            wBosses.applyPos();
        }
        wBosses.setScale(HudConfig.BossScale);
        if (wClan == null) wClan = new WiRewards(true, HudConfig.ClanX, HudConfig.ClanY, null);
        else {
            wClan.setX(HudConfig.ClanX);
            wClan.setY(HudConfig.ClanY);
            wClan.applyPos();
        }
        wClan.setScale(HudConfig.ClanScale);

        for (AdditionalGoals.Type type : AdditionalGoals.Type.values()) {
            WiAdditionalGoal widget = additionalGoalWidgets.computeIfAbsent(type, key -> new WiAdditionalGoal(key, null));
            widget.setX(WiAdditionalGoal.positionX(type));
            widget.setY(WiAdditionalGoal.positionY(type));
            widget.applyPos();
            widget.setScale(WiAdditionalGoal.configuredScale(type));
        }

        hudWidgets.clear();
        hudWidgets.addAll(List.of(wBlockProfitPH, wBlockGoal, wTimeGoal, wGoalNotice, wBosses, wClan));
        hudWidgets.addAll(additionalGoalWidgets.values());

    }

}
