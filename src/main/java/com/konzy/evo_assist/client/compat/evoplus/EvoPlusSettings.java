package com.konzy.evo_assist.client.compat.evoplus;

import static com.konzy.evo_assist.client.util.Texts.tr;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.Config;
import com.konzy.evo_assist.client.config.ConfigChat;
import com.konzy.evo_assist.client.config.ConfigClan;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.config.ConfigMining.bphAllowEnum;
import com.konzy.evo_assist.client.config.ConfigVisual;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals.Type;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import com.konzy.evo_assist.client.features.mine.blockPH.BlockProfitPerHour;
import com.mojang.blaze3d.platform.InputConstants;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import ru.dargen.evoplus.api.addon.Addon;
import ru.dargen.evoplus.api.setting.AddonSettings;
import ru.dargen.evoplus.api.setting.SettingCategory;
import ru.dargen.evoplus.api.setting.SettingContainer;
import ru.dargen.evoplus.api.ui.ButtonStyle;
import ru.dargen.evoplus.api.ui.TextStyle;
import ru.dargen.evoplus.api.ui.Ui;
import ru.dargen.evoplus.api.ui.UiWindow;
import ru.dargen.evoplus.api.setting.type.KeyBind;

/** EvoPlus provides all settings controls; existing files remain the source of saved values. */
public final class EvoPlusSettings {
    private final AddonSettings settings;
    private final Ui ui;
    private final GoalChanges goals = new GoalChanges();
    private final Set<String> scheduledResets = new HashSet<>();
    private final Map<String, UiWindow> resetDialogs = new HashMap<>();

    public EvoPlusSettings(Addon addon) {
        settings = addon.getSettings();
        ui = addon.getUi();
    }

    public void register() {
        var mine = category("mining", "mining");
        reset(mine, "resetMining", "resetMining", "resetMining",
                () -> BlockProfitPerHour.getInstance() != null && BlockProfitPerHour.getInstance().hasStatistics(),
                () -> BlockProfitPerHour.getInstance().reset());
        var sources = mine.section("sources", "evoassist.ui.allowed.title");
        sources.custom("hint", "evoassist.settings.source.title", api -> api.spacer(0, 0))
                .description("evoassist.settings.source.hint");
        for (var source : bphAllowEnum.values()) {
            String key = source == bphAllowEnum.WANDS ? "wands" : source.name().toLowerCase(Locale.ROOT);
            sources.toggle(source.name().toLowerCase(Locale.ROOT), "evoassist.ui." + key, true)
                    .description("evoassist.settings.source." + key)
                    .bind(() -> Arrays.asList(ConfigMining.bphWidgetAllowed).contains(source), enabled -> {
                        var selected = EnumSet.noneOf(bphAllowEnum.class);
                        selected.addAll(Arrays.asList(ConfigMining.bphWidgetAllowed));
                        if (enabled) selected.add(source); else selected.remove(source);
                        ConfigMining.bphWidgetAllowed = selected.toArray(bphAllowEnum[]::new);
                    });
        }

        var miningGoals = category("mining_goals", "mining_goals");
        integerGoal(miningGoals, "blockGoal", "evoassist.section.blocks", 9_999_999,
                () -> ConfigMining.blockGoalTarget, value -> ConfigMining.blockGoalTarget = value, true);
        integerGoal(miningGoals, "timeGoal", "evoassist.section.time", 9_999,
                () -> ConfigMining.timeGoalMinutes, value -> ConfigMining.timeGoalMinutes = value, false);
        amountGoal(miningGoals, "moneyGoal", "evoassist.section.money", Type.MONEY);
        amountGoal(miningGoals, "shardGoal", "evoassist.section.shards", Type.SHARDS);
        var miningGeneral = miningGoals.section("general", "evoassist.section.general");
        miningGeneral.toggle("notifications", "evoassist.ui.goalNotifications.title", true)
                .description("evoassist.ui.goalNotifications.hint")
                .bind(() -> ConfigMining.goalNotifications, value -> ConfigMining.goalNotifications = value);
        reset(miningGeneral, "resetGoals", "resetGoals", "resetGoals",
                () -> MiningGoals.getInstance().hasProgress() || AdditionalGoals.getInstance().hasProgress(false),
                () -> MiningGoals.getInstance().resetAll());

        var bosses = category("bosses", "bosses");
        reset(bosses.section("rewards", "evoassist.section.bossRewards"), "resetBossRewards", "resetRewards", "resetBossRewards",
                () -> EvoAssistClient.rewardStatistics != null && EvoAssistClient.rewardStatistics.hasBossRewardStatistics(),
                () -> EvoAssistClient.rewardStatistics.resetBossRewards());
        reset(bosses.section("tokens", "evoassist.section.bossTokens"), "resetBossTokens", "resetRewards", "resetBossTokens",
                () -> EvoAssistClient.rewardStatistics != null && EvoAssistClient.rewardStatistics.hasBossTokenStatistics(),
                () -> EvoAssistClient.rewardStatistics.resetBossTokens());
        reset(category("kills", "kills"), "resetKills", "resetKills", "resetKills",
                () -> EvoAssistClient.rewardStatistics != null && EvoAssistClient.rewardStatistics.hasKillStatistics(),
                () -> EvoAssistClient.rewardStatistics.resetKills());
        reset(category("clan", "clan"), "resetClan", "resetRewards", "resetClan",
                () -> EvoAssistClient.rewardStatistics != null && EvoAssistClient.rewardStatistics.hasClanStatistics(),
                () -> EvoAssistClient.rewardStatistics.resetClan());

        var clanGoals = category("clan_goals", "clan_goals");
        amountGoal(clanGoals, "clanPointsGoal", "evoassist.section.clanPoints", Type.CLAN_POINTS);
        amountGoal(clanGoals, "clanGoldGoal", "evoassist.section.clanGold", Type.CLAN_GOLD);
        amountGoal(clanGoals, "clanExperienceGoal", "evoassist.section.clanExperience", Type.CLAN_EXPERIENCE);
        var clanGeneral = clanGoals.section("general", "evoassist.section.general");
        clanGeneral.toggle("notifications", "evoassist.ui.goalNotifications.title", true)
                .description("evoassist.ui.clanGoalNotifications.hint")
                .bind(() -> ConfigClan.goalNotifications, value -> ConfigClan.goalNotifications = value);
        reset(clanGeneral, "resetClanGoals", "resetGoals", "resetClanGoals",
                () -> AdditionalGoals.getInstance().hasProgress(true), () -> AdditionalGoals.getInstance().reset(true));

        new EvoPlusFishingSettings(settings, ui).register();

        // The chat category keeps its save path and becomes the combined Interface page.
        var chat = settings.category("chat", "evoassist.page.interface.title");
        chat.enumSelect("orientation", "evoassist.ui.chatOrientation.title", ConfigChat.Orientation.HORIZONTAL)
                .description("evoassist.ui.chatOrientation.hint")
                .names(value -> value == ConfigChat.Orientation.HORIZONTAL ? "evoassist.ui.horizontal" : "evoassist.ui.vertical")
                .bind(() -> ConfigChat.chatTabsOrientation, value -> {
                    ConfigChat.chatTabsOrientation = value;
                    EvoPlusIntegration.invalidateChatHitArea();
                });
        var general = chat.section("general", "evoassist.section.general");
        general.intSlider("notice_duration", "evoassist.ui.goalNoticeDuration.title", 5, 1, 60)
                .description("evoassist.ui.goalNoticeDuration.hint")
                .bind(() -> ConfigVisual.goalNoticeDurationSeconds, value -> ConfigVisual.goalNoticeDurationSeconds = value);
        general.key("menu_key", "evoassist.ui.menuKey.title", KeyBind.keyboard(GLFW.GLFW_KEY_RIGHT_CONTROL))
                .description("evoassist.ui.key.hint").modifiers(false)
                .bind(EvoPlusSettings::menuKey, EvoPlusSettings::menuKey);

        settings.onSave(() -> goals.save((changes, apply) -> Minecraft.getInstance().schedule(
                () -> confirmGoalChanges(changes, apply)), EvoPlusSettings::saveConfig));
    }

    private SettingCategory category(String id, String page) {
        return settings.category(id, "evoassist.page." + page + ".title");
    }

    private void integerGoal(SettingCategory category, String id, String section, int max,
                             IntSupplier get, IntConsumer set, boolean blocks) {
        goals.register(id, () -> Integer.toString(get.getAsInt()), value -> {
            set.accept(Integer.parseInt(value));
            MiningGoals.getInstance().syncTargets();
        }, value -> Integer.toString(Integer.parseInt(value)), value -> (blocks
                ? MiningGoals.getInstance().blockGoal() : MiningGoals.getInstance().timeGoal())
                .wouldResetProgress(Integer.parseInt(value)));
        var container = category.section(id, section);
        container.intField(id + "_target", "evoassist.ui." + id + ".title", 0, 0, max)
                .description("evoassist.ui." + id + ".hint")
                .bind(() -> Integer.parseInt(goals.get(id)), value -> editGoal(id, Integer.toString(value)))
                .visible(() -> false);
        container.custom(id + "_control", "evoassist.ui." + id + ".title", api -> GoalTargetControl.create(
                api, "evoassist.ui.reset." + id + ".title", false, () -> goals.get(id),
                value -> goals.edit(id, value), value -> validInteger(value, max), () -> requestGoalReset(id)))
                .description("evoassist.ui." + id + ".hint");
    }

    private void amountGoal(SettingCategory category, String id, String section, Type type) {
        goals.register(id, () -> AdditionalGoals.configuredTarget(type), value -> {
            AdditionalGoals.setConfiguredTarget(type, value);
            AdditionalGoals.getInstance().syncTargets();
        }, value -> AdditionalGoals.parseTarget(type, value).toPlainString(),
                value -> AdditionalGoals.getInstance().goal(type).wouldResetProgress(new BigDecimal(value)));
        var container = category.section(id, section);
        container.text(id + "_target", "evoassist.ui." + id + ".title", "0")
                .description("evoassist.ui." + id + ".hint").maxLength(32)
                .validator(value -> validAmount(type, value))
                .bind(() -> goals.get(id), value -> editGoal(id, value)).visible(() -> false);
        container.custom(id + "_control", "evoassist.ui." + id + ".title", api -> GoalTargetControl.create(
                api, "evoassist.ui.reset." + id + ".title", true, () -> goals.get(id),
                value -> goals.edit(id, value), value -> validAmount(type, value), () -> requestGoalReset(id)))
                .description("evoassist.ui." + id + ".hint");
    }

    private static boolean validInteger(String value, int max) {
        try { int number = Integer.parseInt(value); return number >= 0 && number <= max; }
        catch (NumberFormatException error) { return false; }
    }

    private static boolean validAmount(Type type, String value) {
        try { AdditionalGoals.parseTarget(type, value); return true; }
        catch (IllegalArgumentException error) { return false; }
    }

    private void editGoal(String id, String value) {
        // Keep the stored native setting's reset behavior consistent with the inline button.
        if (new BigDecimal(goals.normalize(id, value)).signum() != 0
                || new BigDecimal(goals.normalize(id, goals.get(id))).signum() == 0) {
            goals.edit(id, value);
            return;
        }
        requestGoalReset(id);
    }

    private void requestGoalReset(String id) {
        if (new BigDecimal(goals.normalize(id, goals.get(id))).signum() == 0
                && new BigDecimal(goals.normalize(id, goals.current(id))).signum() == 0) return;
        var previousDialog = resetDialogs.get(id);
        if ((previousDialog != null && previousDialog.isOpen()) || !scheduledResets.add(id)) return;
        Minecraft.getInstance().schedule(() -> {
            scheduledResets.remove(id);
            String target = new BigDecimal(goals.normalize(id, goals.current(id))).signum() != 0
                    ? goals.current(id) : goals.get(id);
            String body = tr("evoassist.ui." + id + ".title") + ": " + target + " → 0\n\n"
                    + tr("evoassist.ui.willReset") + "\n" + goalStatistics(id) + "\n\n"
                    + tr("evoassist.ui.resetGoal.hint");
            resetDialogs.put(id, confirm("evoassist.ui.confirmReset", body, "evoassist.ui.reset", () -> {
                goals.reset(id);
                saveConfig();
            }));
        });
    }

    private void reset(SettingContainer container, String id, String title, String description,
                        BooleanSupplier available, Runnable reset) {
        container.button(id, "evoassist.ui." + title + ".title", "evoassist.ui.reset", () -> {
            if (!available.getAsBoolean()) return;
            String body = tr("evoassist.ui." + description + ".description") + "\n\n"
                    + tr("evoassist.ui.willReset") + "\n" + resetStatistics(id);
            confirm("evoassist.ui.confirmReset", body, "evoassist.ui.reset", () -> {
                if (available.getAsBoolean()) reset.run();
            });
        }).description("evoassist.ui." + description + ".hint");
    }

    private void confirmGoalChanges(List<GoalChanges.Change> changes, Runnable apply) {
        String body = changes.stream().map(change -> tr("evoassist.ui." + change.id() + ".title")
                + ": " + change.previous() + " → " + change.next() + "\n"
                + goalStatistics(change.id())).collect(Collectors.joining("\n\n"));
        confirm("evoassist.ui.confirmGoalChange", body + "\n\n" + tr("evoassist.ui.goalChangeWarning"),
                "evoassist.settings.apply", apply);
    }

    private UiWindow confirm(String title, String body, String actionLabel, Runnable action) {
        var window = ui.window(title, 380, 300, true, true);
        window.getContent().add(ui.text(body, TextStyle.BODY).maxWidth(window.getBodyWidth() - 20, true));
        window.getFooter().add(ui.button("evoassist.ui.cancel", window::close),
                ui.button(actionLabel, ButtonStyle.DANGER, () -> {
                    window.close();
                    action.run();
                }));
        return window.open();
    }

    private static String goalStatistics(String id) {
        if (id.equals("blockGoal")) return ResetDetails.goal(MiningGoals.getInstance().blockGoal(), true);
        if (id.equals("timeGoal")) return ResetDetails.goal(MiningGoals.getInstance().timeGoal(), false);
        Type type = switch (id) {
            case "moneyGoal" -> Type.MONEY;
            case "shardGoal" -> Type.SHARDS;
            case "clanPointsGoal" -> Type.CLAN_POINTS;
            case "clanGoldGoal" -> Type.CLAN_GOLD;
            case "clanExperienceGoal" -> Type.CLAN_EXPERIENCE;
            default -> throw new IllegalArgumentException(id);
        };
        return ResetDetails.goal(AdditionalGoals.getInstance().goal(type), type);
    }

    private static String resetStatistics(String id) {
        if (id.equals("resetMining")) {
            var mining = BlockProfitPerHour.getInstance();
            return ResetDetails.mining(mining.totalBrokenBlocks, mining.uptime,
                    BigDecimal.valueOf(mining.totalMoney), mining.totalShards);
        }
        if (id.equals("resetGoals") || id.equals("resetClanGoals")) {
            List<String> ids = id.equals("resetGoals") ? List.of("blockGoal", "timeGoal", "moneyGoal", "shardGoal")
                    : List.of("clanPointsGoal", "clanGoldGoal", "clanExperienceGoal");
            return ids.stream().map(goal -> tr("evoassist.ui." + goal + ".title") + "\n" + goalStatistics(goal))
                    .collect(Collectors.joining("\n\n"));
        }
        var totals = EvoAssistClient.rewardStatistics.current();
        return switch (id) {
            case "resetBossRewards" -> ResetDetails.value("evoassist.hud.money", totals.money) + "\n"
                    + ResetDetails.value("evoassist.hud.shards", totals.shards);
            case "resetBossTokens" -> ResetDetails.value("evoassist.hud.ordinaryTokens", totals.tokens) + "\n"
                    + ResetDetails.value("evoassist.hud.infernalTokens", totals.infernalTokens) + "\n"
                    + ResetDetails.value("evoassist.hud.endTokens", totals.endTokens);
            case "resetKills" -> ResetDetails.value("evoassist.hud.kills", totals.kills);
            case "resetClan" -> ResetDetails.value("evoassist.hud.clanPoints", totals.clanPoints) + "\n"
                    + ResetDetails.value("evoassist.hud.clanExperience", totals.clanExperience) + "\n"
                    + ResetDetails.value("evoassist.hud.clanGold", totals.clanGold);
            default -> throw new IllegalArgumentException(id);
        };
    }

    private static KeyBind menuKey() {
        var key = InputConstants.getKey(EvoAssistClient.menuKey().saveString());
        return key.getType() == InputConstants.Type.MOUSE ? KeyBind.mouse(key.getValue()) : KeyBind.keyboard(key.getValue());
    }

    private static void menuKey(KeyBind binding) {
        var key = binding.isUnbound() ? InputConstants.UNKNOWN
                : (binding.getType() == KeyBind.Type.MOUSE ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM)
                        .getOrCreate(binding.getCode());
        EvoAssistClient.menuKey().setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    private static void saveConfig() { EvoAssistClient.configurator.saveConfig(Config.class); }
}
