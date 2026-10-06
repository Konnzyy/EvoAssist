/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.compat.evoplus;

import static com.konzy.evo_assist.client.util.Texts.tr;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.Config;
import com.konzy.evo_assist.client.config.ConfigClan;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.config.ConfigVisual;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals.Type;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import com.konzy.evo_assist.client.features.mine.blockPH.BlockProfitPerHour;
import com.konzy.evo_assist.client.features.rewards.RewardMessageParser;
import com.konzy.evo_assist.client.features.rewards.RewardStatistics;
import com.konzy.evo_assist.client.util.GoalAmount;
import com.konzy.evo_assist.client.util.MoneyUtils;
import com.konzy.evo_assist.client.util.TimeUtils;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import com.konzy.evo_assist.client.chat.ChatPrefix;
import java.util.function.Consumer;
import org.slf4j.LoggerFactory;
import ru.dargen.evoplus.api.EvoPlusApi;
import ru.dargen.evoplus.api.addon.Addon;
import ru.dargen.evoplus.api.addon.EvoPlusAddon;
import ru.dargen.evoplus.api.data.Notification;
import ru.dargen.evoplus.api.setting.AddonSettings;
import ru.dargen.evoplus.api.setting.SettingContainer;

/** EvoPlus owns widget visibility, layout, scaling and notification delivery. */
public final class EvoPlusIntegration implements EvoPlusAddon {
    private static AddonSettings settings;
    private static EvoPlusChatTabs chatTabs;
    private static boolean widgetMigrationNeedsSave;
    private static boolean lastChatVisibility;

    @Override
    public void onInitialize(Addon addon) {
        settings = addon.getSettings();
        // EvoPlus loads each widget's saved state when it is registered.
        boolean initializeVisibility = !Files.isRegularFile(settings.getFile())
                || !ConfigVisual.widgetsInitializedForEvoPlus;
        registerWidgets(settings, initializeVisibility);
        if (initializeVisibility) {
            settings.save();
            ConfigVisual.widgetsInitializedForEvoPlus = true;
            widgetMigrationNeedsSave = true;
        }
        LoggerFactory.getLogger("EvoAssist").info(
                "Registered 13 EvoAssist widgets in EvoPlus; initialized disabled visibility: {}", initializeVisibility);
    }

    public static void openWidgetSettings() {
        if (settings != null) settings.open();
    }

    public static boolean chatTabsEnabled() { return chatTabs != null && chatTabs.isEnabled(); }

    public static void invalidateChatHitArea() {
        if (chatTabs != null) chatTabs.invalidateHitArea();
    }

    public static boolean clickChatTabs(ChatScreen screen, MouseButtonEvent event, Consumer<ChatPrefix> onPrefix) {
        return chatTabs != null && chatTabs.mouseClicked(screen, event, onPrefix);
    }

    public static void savePendingWidgetMigration() {
        if (!widgetMigrationNeedsSave) return;
        ConfigVisual.widgetsInitializedForEvoPlus = true;
        EvoAssistClient.configurator.saveConfig(Config.class);
        widgetMigrationNeedsSave = false;
    }

    public static void syncChatVisibility() {
        boolean visible = chatTabsEnabled();
        if (visible == lastChatVisibility) return;
        lastChatVisibility = visible;
        invalidateChatHitArea();
        Minecraft.getInstance().gui.hud.getChat().rescaleChat();
    }

    public static void notifyGoal(String message, boolean clan) {
        if (!(clan ? ConfigClan.goalNotifications : ConfigMining.goalNotifications)) return;
        Notification notification = goalNotification(message);
        Minecraft.getInstance().execute(() -> EvoPlusApi.showNotification(notification));
    }

    public static Notification goalNotification(String message) {
        return Notification.builder().title("EvoAssist").message(message)
                .duration(Duration.ofMillis(ConfigVisual.goalNoticeDurationMillis())).build();
    }

    // Separate registration from game state so registration and persistence can be checked without a client.
    public static void registerWidgets(AddonSettings settings, boolean initializeVisibility) {
        var mining = settings.category("mining", "evoassist.page.mining.title");
        widget(mining, "mining_statistics", "miningWidget", initializeVisibility,
                EvoPlusIntegration::inWorld, EvoPlusIntegration::miningContent);
        var goals = settings.category("mining_goals", "evoassist.page.mining_goals.title");
        widget(goals, "block_goal", "blockGoalWidget", initializeVisibility,
                () -> inWorld() && MiningGoals.getInstance().blockGoal().active(), () -> miningGoalContent(true));
        widget(goals, "time_goal", "timeGoalWidget", initializeVisibility,
                () -> inWorld() && MiningGoals.getInstance().timeGoal().active(), () -> miningGoalContent(false));
        additionalWidget(goals, Type.MONEY, initializeVisibility);
        additionalWidget(goals, Type.SHARDS, initializeVisibility);
        var bosses = settings.category("bosses", "evoassist.page.bosses.title");
        widget(bosses, "boss_rewards", "bossWidget", initializeVisibility,
                EvoPlusIntegration::rewardsAvailable, () -> rewardsContent(false));
        widget(bosses, "boss_tokens", "bossTokensWidget", initializeVisibility,
                EvoPlusIntegration::rewardsAvailable, EvoPlusIntegration::tokensContent);
        var kills = settings.category("kills", "evoassist.page.kills.title");
        widget(kills, "player_kills", "killsWidget", initializeVisibility,
                EvoPlusIntegration::rewardsAvailable, EvoPlusIntegration::killsContent);
        var clan = settings.category("clan", "evoassist.page.clan.title");
        widget(clan, "clan_statistics", "clanWidget", initializeVisibility,
                EvoPlusIntegration::rewardsAvailable, () -> rewardsContent(true));
        var clanGoals = settings.category("clan_goals", "evoassist.page.clan_goals.title");
        additionalWidget(clanGoals, Type.CLAN_POINTS, initializeVisibility);
        additionalWidget(clanGoals, Type.CLAN_GOLD, initializeVisibility);
        additionalWidget(clanGoals, Type.CLAN_EXPERIENCE, initializeVisibility);
        var chat = settings.category("chat", "evoassist.section.chat");
        chatTabs = new EvoPlusChatTabs(chat, initializeVisibility, false);
    }

    private static void widget(SettingContainer category, String id, String key, boolean initializeVisibility,
                               BooleanSupplier available, Supplier<EvoPlusWidget.Content> content) {
        // Let EvoPlus translate metadata when displayed, including after a language switch.
        new EvoPlusWidget(category, id, "evoassist.ui." + key + ".title",
                "evoassist.ui." + key + ".hint", initializeVisibility, false, available, content);
    }

    private static void additionalWidget(SettingContainer category, Type type, boolean initializeVisibility) {
        String key = switch (type) {
            case MONEY -> "moneyGoalWidget";
            case SHARDS -> "shardGoalWidget";
            case CLAN_POINTS -> "clanPointsGoalWidget";
            case CLAN_GOLD -> "clanGoldGoalWidget";
            case CLAN_EXPERIENCE -> "clanExperienceGoalWidget";
        };
        widget(category, type.name().toLowerCase(java.util.Locale.ROOT) + "_goal", key, initializeVisibility,
                () -> inWorld() && AdditionalGoals.getInstance().goal(type).active()
                        && (!type.clan || AdditionalGoals.getInstance().connected()),
                () -> additionalGoalContent(type));
    }

    private static boolean inWorld() {
        var client = Minecraft.getInstance();
        return client.player != null && client.level != null;
    }

    private static boolean rewardsAvailable() {
        return inWorld() && EvoAssistClient.rewardStatistics != null && EvoAssistClient.rewardStatistics.isConnected();
    }

    private static EvoPlusWidget.Content content(String texture, int size, String... lines) {
        return new EvoPlusWidget.Content("minecraft:textures/" + texture + ".png", size, List.of(lines));
    }

    private static String mined(long blocks, long money, long shards) {
        return tr("evoassist.hud.mined") + "§b" + MoneyUtils.convertTo(blocks) + "§f\uE127 / §a"
                + MoneyUtils.convertTo(money) + "§f\uE135 / §d" + MoneyUtils.convertTo(shards) + "§f\uE365";
    }

    private static EvoPlusWidget.Content miningContent() {
        var counter = BlockProfitPerHour.getInstance();
        return content("item/netherite_pickaxe", 28,
                tr("evoassist.hud.time") + TimeUtils.asTextTime(counter == null ? 0 : counter.uptime),
                tr("evoassist.hud.perHour") + "§b" + MoneyUtils.convertTo(counter == null ? 0 : counter.BlocksPerHour)
                        + "§f\uE127 / §a" + MoneyUtils.convertTo(counter == null ? 0 : counter.MoneyPerHour)
                        + "§f\uE135 / §d" + MoneyUtils.convertTo(counter == null ? 0 : counter.ShardsPerHour) + "§f\uE365",
                mined(counter == null ? 0 : counter.totalBrokenBlocks, counter == null ? 0 : counter.totalMoney,
                        counter == null ? 0 : counter.totalShards));
    }

    private static EvoPlusWidget.Content miningGoalContent(boolean blocks) {
        var goals = MiningGoals.getInstance();
        var goal = blocks ? goals.blockGoal() : goals.timeGoal();
        String complete = goal.complete ? " §a✓" : "";
        if (blocks) return content("item/netherite_pickaxe", 20,
                tr("evoassist.hud.blocks") + "§b" + goal.blocks + "§f / " + (goal.active() ? goal.target() : 10_000) + complete,
                tr("evoassist.hud.time") + TimeUtils.asTextTime(goal.activeMillis),
                tr("evoassist.hud.mined") + "§a" + MoneyUtils.convertTo(goal.money)
                        + "§f\uE135 / §d" + MoneyUtils.convertTo(goal.shards) + "§f\uE365");
        return content("item/netherite_pickaxe", 20,
                tr("evoassist.hud.time") + TimeUtils.asTextTime(goal.activeMillis) + " / "
                        + TimeUtils.asTextTime((goal.active() ? goal.target() : 60) * 60_000L) + complete,
                mined(goal.blocks, goal.money, goal.shards));
    }

    private static EvoPlusWidget.Content additionalGoalContent(Type type) {
        var goal = AdditionalGoals.getInstance().goal(type);
        String texture = switch (type) {
            case MONEY, SHARDS -> "item/netherite_pickaxe";
            case CLAN_POINTS -> "block/sunflower_front";
            case CLAN_GOLD -> "item/gold_ingot";
            case CLAN_EXPERIENCE -> "item/experience_bottle";
        };
        String color = switch (type) {
            case MONEY, CLAN_EXPERIENCE -> "§a";
            case SHARDS -> "§d";
            case CLAN_POINTS -> "§b";
            case CLAN_GOLD -> "§e";
        };
        BigDecimal preview = BigDecimal.valueOf(switch (type) {
            case MONEY -> 5_000_000_000L;
            case SHARDS -> 1000;
            case CLAN_POINTS -> 5000;
            case CLAN_GOLD -> 100;
            case CLAN_EXPERIENCE -> 50;
        });
        String icon = type == Type.MONEY ? "\uE135" : type == Type.SHARDS ? "\uE365" : "";
        String progress = (type.clan ? tr(type.title) : tr("evoassist.hud.received")) + ": " + color
                + AdditionalGoals.format(type, goal.progress) + "§f" + icon + " / " + color
                + AdditionalGoals.format(type, goal.active() ? goal.target : preview) + "§f" + icon
                + (goal.complete ? " §a✓" : "");
        if (type.clan) return content(texture, 12, progress);
        String secondary = type == Type.MONEY ? "§d" + GoalAmount.format(BigDecimal.valueOf(goal.shards)) + "§f\uE365"
                : "§a" + GoalAmount.format(goal.money) + "§f\uE135";
        return content(texture, 20, progress, tr("evoassist.hud.time") + TimeUtils.asTextTime(goal.activeMillis),
                tr("evoassist.hud.mined") + "§b" + GoalAmount.format(BigDecimal.valueOf(goal.blocks)) + "§f\uE127 / " + secondary);
    }

    private static RewardStatistics.Totals totals() {
        return EvoAssistClient.rewardStatistics == null ? new RewardStatistics.Totals() : EvoAssistClient.rewardStatistics.current();
    }

    private static EvoPlusWidget.Content rewardsContent(boolean clan) {
        var totals = totals();
        if (clan) return content("item/gold_ingot", 24,
                tr("evoassist.hud.clanPoints") + "§b" + RewardMessageParser.whole(totals.clanPoints),
                tr("evoassist.hud.clanExperience") + "§a" + RewardMessageParser.whole(totals.clanExperience),
                tr("evoassist.hud.clanGold") + "§e" + RewardMessageParser.whole(totals.clanGold));
        return content("item/netherite_sword", 20,
                tr("evoassist.hud.money") + "§a" + RewardMessageParser.compactMoney(totals.money) + "§f\uE135",
                tr("evoassist.hud.shards") + "§d" + RewardMessageParser.whole(totals.shards) + "§f\uE365");
    }

    private static EvoPlusWidget.Content killsContent() {
        return content("item/diamond_sword", 16,
                tr("evoassist.hud.kills") + "§b" + RewardMessageParser.whole(totals().kills));
    }

    private static EvoPlusWidget.Content tokensContent() {
        var totals = totals();
        return content("item/nether_star", 24,
                tr("evoassist.hud.ordinaryTokens") + "§e" + RewardMessageParser.whole(totals.tokens),
                tr("evoassist.hud.infernalTokens") + "§c" + RewardMessageParser.whole(totals.infernalTokens),
                tr("evoassist.hud.endTokens") + "§d" + RewardMessageParser.whole(totals.endTokens));
    }
}
