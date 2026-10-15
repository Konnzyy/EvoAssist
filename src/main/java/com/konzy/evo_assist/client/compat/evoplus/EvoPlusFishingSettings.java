package com.konzy.evo_assist.client.compat.evoplus;

import static com.konzy.evo_assist.client.util.Texts.tr;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.Config;
import com.konzy.evo_assist.client.config.ConfigFishing;
import com.konzy.evo_assist.client.features.fishing.FishingData.Rarity;
import com.konzy.evo_assist.client.features.fishing.FishingData.World;
import com.konzy.evo_assist.client.features.fishing.FishingGoals.Goal;
import com.konzy.evo_assist.client.features.fishing.FishingInventory;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.minecraft.client.Minecraft;
import ru.dargen.evoplus.api.setting.AddonSettings;
import ru.dargen.evoplus.api.ui.ButtonStyle;
import ru.dargen.evoplus.api.ui.TextStyle;
import ru.dargen.evoplus.api.ui.Ui;

/** Native EvoPlus controls; editing inputs never changes an already active fishing goal. */
final class EvoPlusFishingSettings {
    private final AddonSettings settings;
    private final Ui ui;
    EvoPlusFishingSettings(AddonSettings settings, Ui ui) { this.settings = settings; this.ui = ui; }

    void register() {
        ConfigFishing.normalizeLevels();
        var category = settings.category("fishing_goals", "evoassist.page.fishing_goals.title");
        var pet = category.section("pet", "evoassist.fishing.pet");
        pet.enumSelect("world", "evoassist.fishing.world", World.OVERWORLD)
                .description("evoassist.fishing.world.hint").names(World::label)
                .bind(() -> ConfigFishing.world, value -> {
                    ConfigFishing.world = value;
                    ConfigFishing.normalizeLevels();
                });
        pet.enumSelect("rarity", "evoassist.fishing.rarity", Rarity.COMMON)
                .description("evoassist.fishing.rarity.hint").names(Rarity::label)
                .bind(() -> ConfigFishing.rarity, value -> ConfigFishing.rarity = value);
        pet.choice("current_level", "evoassist.fishing.currentLevel", "1", () ->
                        IntStream.range(ConfigFishing.world.firstLevel(), ConfigFishing.world.maxLevel())
                                .mapToObj(Integer::toString).toList())
                .description("evoassist.fishing.currentLevel.hint")
                .bind(() -> Integer.toString(ConfigFishing.currentLevel), value -> {
                    ConfigFishing.currentLevel = Integer.parseInt(value);
                    ConfigFishing.normalizeLevels();
                });
        pet.intField("current_experience", "evoassist.fishing.currentExperience", 0, 0, Integer.MAX_VALUE)
                .description("evoassist.fishing.currentExperience.hint")
                .bind(() -> ConfigFishing.currentExperience, value -> ConfigFishing.currentExperience = value);
        pet.choice("target_level", "evoassist.fishing.targetLevel", "2", () ->
                        IntStream.rangeClosed(ConfigFishing.currentLevel + 1, ConfigFishing.world.maxLevel())
                                .mapToObj(Integer::toString).toList())
                .description("evoassist.fishing.targetLevel.hint")
                .bind(() -> Integer.toString(ConfigFishing.targetLevel), value -> {
                    ConfigFishing.targetLevel = Integer.parseInt(value);
                    ConfigFishing.normalizeLevels();
                });
        pet.button("set_pet_goal", "evoassist.fishing.setPet", "evoassist.fishing.setPet", () -> apply(() ->
                        Goal.level(ConfigFishing.world, ConfigFishing.rarity, ConfigFishing.currentLevel,
                                ConfigFishing.targetLevel, ConfigFishing.currentExperience), "evoassist.fishing.invalid"))
                .description("evoassist.fishing.setPet.hint");

        var custom = category.section("custom", "evoassist.fishing.custom");
        custom.enumSelect("world", "evoassist.fishing.world", World.OVERWORLD)
                .description("evoassist.fishing.world.hint").names(World::label)
                .bind(() -> ConfigFishing.customWorld, value -> ConfigFishing.customWorld = value);
        custom.text("experience", "evoassist.fishing.customExperience", "0")
                .description("evoassist.fishing.customExperience.hint").maxLength(19)
                .validator(EvoPlusFishingSettings::validDraft)
                .bind(() -> ConfigFishing.customExperience, value -> ConfigFishing.customExperience = value);
        custom.button("set_custom_goal", "evoassist.fishing.setCustom", "evoassist.fishing.setCustom", () -> apply(() ->
                        Goal.custom(ConfigFishing.customWorld, Long.parseLong(ConfigFishing.customExperience)),
                        "evoassist.fishing.invalidCustom"))
                .description("evoassist.fishing.setCustom.hint");

        var general = category.section("general", "evoassist.section.general");
        general.toggle("include_buckets", "evoassist.fishing.includeBuckets", true)
                .description("evoassist.fishing.includeBuckets.hint")
                .bind(() -> ConfigFishing.includeBuckets, value -> ConfigFishing.includeBuckets = value);
        general.toggle("notifications", "evoassist.ui.goalNotifications.title", true)
                .description("evoassist.fishing.notifications.hint")
                .bind(() -> ConfigFishing.goalNotifications, value -> ConfigFishing.goalNotifications = value);
        general.button("clear_goal", "evoassist.fishing.clear", "evoassist.ui.reset", () -> {
            var goals = FishingInventory.goals();
            if (goals.goal() == null) return;
            var window = ui.window("evoassist.ui.confirmReset", 380, 280, true, true);
            window.getContent().add(ui.text(tr("evoassist.fishing.clear.hint") + "\n\n"
                    + tr("evoassist.fishing.targetExperience", goals.goal().targetExperience()) + "\n"
                    + tr("evoassist.fishing.inventory", goals.progress(), goals.goal().targetExperience()), TextStyle.BODY)
                    .maxWidth(window.getBodyWidth() - 20, true));
            window.getFooter().add(ui.button("evoassist.ui.cancel", window::close),
                    ui.button("evoassist.ui.reset", ButtonStyle.DANGER, () -> { goals.reset(); window.close(); }));
            window.open();
        }).description("evoassist.fishing.clear.hint");
    }

    private static boolean validDraft(String value) {
        if (value.isEmpty()) return true;
        if (!value.matches("[0-9]{1,19}")) return false;
        try { return Long.parseLong(value) >= 0; }
        catch (NumberFormatException ignored) { return false; }
    }

    private void apply(Supplier<Goal> create, String errorKey) {
        var goals = FishingInventory.goals();
        if (!goals.connected()) { message("evoassist.fishing.error", tr("evoassist.fishing.join")); return; }
        Goal goal;
        try { goal = create.get(); }
        catch (IllegalArgumentException error) {
            message("evoassist.fishing.error", tr(errorKey));
            return;
        }
        FishingInventory.refresh(Minecraft.getInstance());
        goals.setGoal(goal);
        EvoAssistClient.configurator.saveConfig(Config.class);
        message("evoassist.fishing.set", tr("evoassist.fishing.targetExperience", goal.targetExperience()) + "\n"
                + tr("evoassist.fishing.inventory", goals.progress(), goal.targetExperience()) + "\n"
                + tr("evoassist.fishing.remaining", goals.remaining()));
    }

    private void message(String title, String body) {
        var window = ui.window(title, 380, 240, true, true);
        window.getContent().add(ui.text(body, TextStyle.BODY).maxWidth(window.getBodyWidth() - 20, true));
        window.getFooter().add(ui.button("evoassist.ui.done", window::close));
        window.open();
    }
}
