/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client;

import com.konzy.evo_assist.client.chat.ChatTabManager;
import com.konzy.evo_assist.client.compat.evoplus.EvoPlusIntegration;
import com.konzy.evo_assist.client.config.Config;
import com.konzy.evo_assist.client.event.ChatGameEvent;
import com.konzy.evo_assist.client.features.mine.blockPH.BlockProfitPerHour;
import com.konzy.evo_assist.client.features.mine.blockPH.MiningStatistics;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import com.konzy.evo_assist.client.features.rewards.RewardStatistics;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import net.fabricmc.loader.api.FabricLoader;
import com.konzy.evo_assist.client.ui.EvoConfigScreen;
import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;


public class EvoAssistClient implements ClientModInitializer {

    public static final Logger logger = LoggerFactory.getLogger("EvoAssist");
    public static final String MODID = "evoassist";
    public static final Configurator configurator = new Configurator(MODID);


    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MODID, "controls"));
    private static final KeyMapping ConfigBind = new KeyMapping("key.evoassist.open_menu", GLFW.GLFW_KEY_RIGHT_CONTROL, CATEGORY);

    public static Minecraft instance;
    public static EvoAssistClient evoClient;

    public static RewardStatistics rewardStatistics;
    private boolean chatFilterInstalled = false;
    public static BlockProfitPerHour eventBlockProfitPerHour;
    public static ChatGameEvent eventChatGame;
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
                error -> logger.warn("Could not update reward/kill statistics", error),
                reward -> AdditionalGoals.getInstance().reward(reward));
        MiningGoals.getInstance();
        AdditionalGoals.getInstance();
        ChatTabManager.getInstance();

        //KeyBinds

        KeyMappingHelper.registerKeyMapping(ConfigBind);

        // Event register

        eventBlockProfitPerHour = new BlockProfitPerHour(new MiningStatistics(
                FabricLoader.getInstance().getConfigDir().resolve("evoassist_mining.properties"),
                error -> logger.warn("Could not update mining statistics", error)));
        eventChatGame = new ChatGameEvent();
        ClientPlayerBlockBreakEvents.AFTER.register(eventBlockProfitPerHour);
        ClientReceiveMessageEvents.GAME.register(eventChatGame);
        ClientPlayConnectionEvents.JOIN.register((_, _, client) -> {
            var server = client.getCurrentServer();
            String miningServer = server != null ? server.ip : client.getSingleplayerServer() != null
                    ? "singleplayer:" + client.getSingleplayerServer().getWorldPath(
                            net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize()
                    : "";
            eventBlockProfitPerHour.connect(miningServer);
            MiningGoals.getInstance().expirePendingPrices();
            if (server != null) {
                rewardStatistics.connect(server.ip);
                AdditionalGoals.getInstance().connect(server.ip);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> {
            eventBlockProfitPerHour.disconnect();
            rewardStatistics.disconnect();
            AdditionalGoals.getInstance().disconnect();
            MiningGoals.getInstance().expirePendingPrices();
            MiningGoals.getInstance().save();
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
                instance.schedule(EvoPlusIntegration::openWidgetSettings);
                return 1;
            }));
            commandDispatcher.register(ClientCommands.literal("eaw").executes(commandContext -> {
                instance.schedule(EvoPlusIntegration::openWidgetSettings);
                return 1;
            }));
        }));

        AtomicLong latestSecond = new AtomicLong(System.currentTimeMillis());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EvoPlusIntegration.savePendingWidgetMigration();
            EvoPlusIntegration.syncChatVisibility();
            if (!chatFilterInstalled) {
                client.gui.hud.getChat().setVisibleMessageFilter(message ->
                        !EvoPlusIntegration.chatTabsEnabled()
                                || ChatTabManager.getInstance().shouldDisplayMessage(message.content()));
                chatFilterInstalled = true;
            }
            if(System.currentTimeMillis() - latestSecond.get() >= 1000) {
                eventBlockProfitPerHour.second();

                latestSecond.set(System.currentTimeMillis());
            }


            while (ConfigBind.consumeClick()) {
                instance.gui.setScreen(new EvoConfigScreen(instance.gui.screen()));
            }

        });

    }
}
