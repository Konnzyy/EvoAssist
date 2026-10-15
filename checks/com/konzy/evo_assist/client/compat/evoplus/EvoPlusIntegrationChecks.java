package com.konzy.evo_assist.client.compat.evoplus;

import com.konzy.evo_assist.client.config.ConfigBosses;
import com.konzy.evo_assist.client.config.ConfigChat;
import com.konzy.evo_assist.client.config.ConfigClan;
import com.konzy.evo_assist.client.config.ConfigFishing;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.config.ConfigVisual;
import com.konzy.evo_assist.client.util.Texts;
import com.konzy.evo_assist.client.features.fishing.FishingData;
import com.konzy.evo_assist.client.features.fishing.FishingGoals;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BooleanSupplier;
import ru.dargen.evoplus.api.ui.Ui;
import ru.dargen.evoplus.api.ui.UiElement;
import ru.dargen.evoplus.api.addon.Addon;
import ru.dargen.evoplus.api.render.RenderContext;
import ru.dargen.evoplus.api.render.WidgetRenderer;
import ru.dargen.evoplus.api.setting.AddonSettings;
import ru.dargen.evoplus.api.setting.SettingCategory;
import ru.dargen.evoplus.api.setting.SettingSection;
import ru.dargen.evoplus.api.setting.type.WidgetElement;

/** Exercises the public API contract without starting Minecraft or writing game settings. */
public final class EvoPlusIntegrationChecks {
    private static int assertions;
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static final class WidgetState {
        String name, description;
        boolean enabled;
        int enabledWrites;
        double width, height;
        boolean chatMovable = true;
        WidgetRenderer renderer;
        final WidgetElement element = proxy(WidgetElement.class, (self, method, args) -> switch (method.getName()) {
            case "description" -> { description = (String) args[0]; yield self; }
            case "size" -> { width = (double) args[0]; height = (double) args[1]; yield self; }
            case "isEnabled" -> enabled;
            case "setEnabled" -> { enabled = (boolean) args[0]; enabledWrites++; yield null; }
            case "getWidth" -> width;
            case "getHeight" -> height;
            case "chatMovable" -> { chatMovable = (boolean) args[0]; yield self; }
            default -> throw new AssertionError("Unexpected widget API: " + method);
        });
    }

    private static final class ControlState {
        String name, description;
        Supplier<List<String>> options;
        Supplier<Object> get;
        Consumer<Object> set;
        Runnable action;
        Function<Ui, UiElement<?>> factory;
        BooleanSupplier visible = () -> true;
        Object element;
    }

    private static final class SettingsState {
        final Map<String, WidgetState> widgets = new LinkedHashMap<>();
        final Map<String, String> categories = new LinkedHashMap<>();
        final Map<String, Object> containers = new LinkedHashMap<>();
        final Map<String, ControlState> controls = new LinkedHashMap<>();
        Path file;
        int saves, opens;
        boolean savedVisibility;
        final AddonSettings settings = proxy(AddonSettings.class, (self, method, args) -> switch (method.getName()) {
            case "getFile" -> file;
            case "save" -> { saves++; yield null; }
            case "open" -> { opens++; yield null; }
            case "onSave" -> self;
            case "category" -> {
                String id = (String) args[0];
                categories.putIfAbsent(id, (String) args[1]);
                yield container(id, SettingCategory.class);
            }
            default -> throw new AssertionError("Unexpected settings API: " + method);
        });

        @SuppressWarnings("unchecked")
        private <T> T container(String path, Class<T> type) {
            return type.cast(containers.computeIfAbsent(path, key -> proxy(type, (container, operation, values) -> {
                if (operation.getName().equals("section")) return container(path + "/" + values[0], SettingSection.class);
                if (operation.getName().equals("widget")) {
                    String widgetId = (String) values[0];
                    var state = new WidgetState();
                    state.enabled = savedVisibility;
                    state.name = (String) values[1];
                    state.width = (double) values[2]; state.height = (double) values[3];
                    state.renderer = (WidgetRenderer) values[4];
                    check(widgets.put(widgetId, state) == null, "Unique widget " + widgetId);
                    return state.element;
                }
                if (Set.of("toggle", "intField", "text", "enumSelect", "choice", "intSlider", "key", "button", "custom").contains(operation.getName())) {
                    var control = new ControlState();
                    control.name = (String) values[1];
                    if (operation.getName().equals("choice")) control.options = (Supplier<List<String>>) values[3];
                    if (operation.getName().equals("button")) control.action = (Runnable) values[3];
                    if (operation.getName().equals("custom")) control.factory = (Function<Ui, UiElement<?>>) values[2];
                    control.element = proxy(operation.getReturnType(), (self, method, args) -> switch (method.getName()) {
                        case "description" -> { control.description = (String) args[0]; yield self; }
                        case "bind" -> { control.get = (Supplier<Object>) args[0]; control.set = (Consumer<Object>) args[1]; yield self; }
                        case "visible" -> { control.visible = (BooleanSupplier) args[0]; yield self; }
                        case "names", "modifiers", "maxLength", "validator" -> self;
                        default -> throw new AssertionError("Unexpected native control API: " + method);
                    });
                    check(controls.put(path + "/" + values[0], control) == null, "Unique setting " + path + "/" + values[0]);
                    return control.element;
                }
                throw new AssertionError(operation);
            })));
        }
        final Addon addon = proxy(Addon.class, (self, method, args) -> {
            if (method.getName().equals("getSettings")) return settings;
            if (method.getName().equals("getUi")) return null; // Registration does not open UI windows.
            throw new AssertionError(method);
        });
    }

    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[0]);
        Files.createDirectories(directory);
        Path freshFile = directory.resolve("fresh-addon.json");
        Files.deleteIfExists(freshFile);
        var first = new SettingsState(); first.file = freshFile;
        ConfigVisual.widgetsInitializedForEvoPlus = false;
        ConfigMining.bphWidgetToggle = true;
        ConfigMining.blockGoalWidgetEnabled = false;
        ConfigBosses.widgetEnabled = true;
        ConfigBosses.tokensWidgetEnabled = false;
        ConfigClan.widgetEnabled = true;
        ConfigChat.chatTabsToggle = true;
        ConfigChat.chatWidgetMigratedToEvoPlus = false;
        new EvoPlusIntegration().onInitialize(first.addon);
        check(first.widgets.keySet().equals(Set.of("mining_statistics", "block_goal", "time_goal", "money_goal",
                "shards_goal", "boss_rewards", "boss_tokens", "clan_statistics", "clan_points_goal",
                "clan_gold_goal", "clan_experience_goal", "chat_tabs", "player_kills", "fishing_goal")), "Every HUD widget registered");
        check(first.categories.size() == 8 && !first.categories.containsKey("calculator"), "Eight native categories including fishing goals");
        check(first.categories.get("chat").equals("evoassist.page.interface.title") && !first.categories.containsKey("interface"),
                "Chat and general controls share Interface while retaining the persisted chat widget path");
        check(first.categories.values().stream().noneMatch(name -> Texts.tr(name).equals(name)), "Category translations exist");
        for (var widget : first.widgets.values()) {
            check(widget.enabledWrites == 1, "Visibility initialized once");
            check(!widget.enabled, "Every widget starts disabled despite legacy flags");
            check(!Texts.tr(widget.name).equals(widget.name) && !Texts.tr(widget.description).equals(widget.description), "Localized widget metadata");
        }
        check(ConfigVisual.widgetsInitializedForEvoPlus, "One-time initialization marker set");
        check(!EvoPlusIntegration.chatTabsEnabled(), "Chat panel starts disabled too");
        first.widgets.get("chat_tabs").enabled = true;
        check(EvoPlusIntegration.chatTabsEnabled(), "Enabling in EvoPlus enables chat controls");
        first.widgets.get("chat_tabs").enabled = false;
        check(!EvoPlusIntegration.chatTabsEnabled(), "Disabling in EvoPlus disables chat controls");
        check(!first.widgets.get("player_kills").enabled, "New kills widget starts disabled for manual placement");
        check(!first.widgets.get("chat_tabs").chatMovable, "Chat controls default to receiving clicks instead of dragging");
        check(first.controls.size() == 46, "Existing controls and twelve fishing controls registered");
        for (var id : List.of("blockGoal", "timeGoal", "moneyGoal", "shardGoal", "clanPointsGoal", "clanGoldGoal", "clanExperienceGoal")) {
            String path = (id.startsWith("clan") ? "clan_goals/" : "mining_goals/") + id + "/" + id;
            check(!first.controls.get(path + "_target").visible.getAsBoolean(), "No duplicate native field for " + id);
            check(first.controls.get(path + "_control").factory != null
                    && first.controls.get(path + "_control").visible.getAsBoolean(), "Inline reset and input available for " + id);
        }
        check(first.controls.keySet().stream().noneMatch(key -> key.endsWith("_reset")), "Individual goal reset cards removed");
        for (var control : first.controls.values()) {
            check(!Texts.tr(control.name).equals(control.name), "Localized native control: " + control.name);
            check(!Texts.tr(control.description).equals(control.description), "Localized native description: " + control.description);
            check(control.action != null || control.factory != null
                    || (control.get != null && control.set != null), "Every value uses existing config storage");
        }
        var petWorld = first.controls.get("fishing_goals/pet/world");
        var buckets = first.controls.get("fishing_goals/general/include_buckets");
        check(Boolean.TRUE.equals(buckets.get.get()), "Bucket experience starts enabled");
        buckets.set.accept(false);
        check(!ConfigFishing.includeBuckets, "Native toggle disables bucket counting in config");
        buckets.set.accept(true);
        check(ConfigFishing.includeBuckets, "Native toggle can enable bucket counting again");
        var customWorld = first.controls.get("fishing_goals/custom/world");
        var currentLevel = first.controls.get("fishing_goals/pet/current_level");
        var targetLevel = first.controls.get("fishing_goals/pet/target_level");
        check(currentLevel.options != null && targetLevel.options != null, "Both pet levels use native choices");
        petWorld.set.accept(FishingData.World.OVERWORLD);
        check(currentLevel.options.get().equals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"))
                && targetLevel.options.get().getLast().equals("11"), "Overworld choices end at the transition to level 11");
        currentLevel.set.accept("10");
        check(targetLevel.options.get().equals(List.of("11")) && targetLevel.get.get().equals("11"),
                "The last Overworld level has exactly one valid target");
        customWorld.set.accept(FishingData.World.END);
        petWorld.set.accept(FishingData.World.NETHER);
        check(currentLevel.options.get().equals(List.of("11", "12", "13", "14", "15"))
                && currentLevel.get.get().equals("11") && targetLevel.get.get().equals("12"),
                "Changing to Nether updates both choices and their selected values");
        currentLevel.set.accept("15");
        check(targetLevel.options.get().equals(List.of("16")) && targetLevel.get.get().equals("16"),
                "Nether choices stop at level 16");
        petWorld.set.accept(FishingData.World.END);
        check(currentLevel.options.get().equals(List.of("16", "17", "18", "19", "20"))
                && targetLevel.options.get().equals(List.of("17", "18", "19", "20", "21")),
                "End choices cover the final five transitions");
        targetLevel.set.accept("21");
        petWorld.set.accept(FishingData.World.OVERWORLD);
        check(currentLevel.get.get().equals("10") && targetLevel.get.get().equals("11"),
                "Returning to Overworld removes out-of-range selected levels");
        check(customWorld.get.get() == FishingData.World.END && ConfigFishing.customWorld == FishingData.World.END,
                "Experience goal world stays independent from the pet goal world");
        ConfigFishing.currentLevel = 1; ConfigFishing.targetLevel = 2; ConfigFishing.customWorld = FishingData.World.OVERWORLD;
        ConfigMining.bphWidgetAllowed = new ConfigMining.bphAllowEnum[] { ConfigMining.bphAllowEnum.BARRELS };
        var blockSource = first.controls.get("mining/sources/blocks");
        check(Boolean.FALSE.equals(blockSource.get.get()), "Native control reads existing source selection");
        blockSource.set.accept(true);
        check(java.util.Arrays.asList(ConfigMining.bphWidgetAllowed).containsAll(List.of(
                ConfigMining.bphAllowEnum.BARRELS, ConfigMining.bphAllowEnum.BLOCKS)), "Source toggle preserves other selections");
        blockSource.set.accept(false);
        check(ConfigMining.bphWidgetAllowed.length == 1 && ConfigMining.bphWidgetAllowed[0] == ConfigMining.bphAllowEnum.BARRELS,
                "Turning a source off removes only that source");
        ConfigMining.blockGoalTarget = 1234;
        var targetField = first.controls.get("mining_goals/blockGoal/blockGoal_target");
        check(targetField.get.get().equals(1234), "Existing target appears in native input");
        targetField.set.accept(5678);
        check(ConfigMining.blockGoalTarget == 1234 && targetField.get.get().equals(5678), "Typing cannot reset an active goal before saving");
        check(first.saves == 1, "Migration persisted by EvoPlus");
        EvoPlusIntegration.openWidgetSettings();
        check(first.opens == 1, "Settings shortcut uses EvoPlus");

        Path savedFile = directory.resolve("existing-addon.json");
        Files.writeString(savedFile, "{}");
        ConfigVisual.widgetsInitializedForEvoPlus = false;
        ConfigChat.chatWidgetMigratedToEvoPlus = true;
        var upgrade = new SettingsState(); upgrade.file = savedFile; upgrade.savedVisibility = true;
        new EvoPlusIntegration().onInitialize(upgrade.addon);
        check(upgrade.widgets.values().stream().allMatch(widget -> widget.enabledWrites == 1 && !widget.enabled),
                "Existing integration users have all saved enabled widgets disabled once");
        check(upgrade.saves == 1 && ConfigVisual.widgetsInitializedForEvoPlus,
                "Upgrade persisted by EvoPlus and marked complete");

        ConfigChat.chatWidgetMigratedToEvoPlus = false;
        ConfigChat.chatTabsToggle = false;
        var subsequent = new SettingsState(); subsequent.file = savedFile; subsequent.savedVisibility = true;
        new EvoPlusIntegration().onInitialize(subsequent.addon);
        check(subsequent.widgets.values().stream().allMatch(widget -> widget.enabledWrites == 0 && widget.enabled),
                "Reopening preserves user-enabled widgets, ignoring legacy visibility and migration flags");
        check(EvoPlusIntegration.chatTabsEnabled(), "Saved chat visibility also preserved");
        check(subsequent.saves == 0, "Registration does not rewrite existing settings");

        var rendererSettings = new SettingsState();
        var category = rendererSettings.settings.category("render", "Rendering");
        var available = new AtomicBoolean(false);
        var value = new AtomicReference<>(new EvoPlusWidget.Content("minecraft:textures/item/netherite_pickaxe.png", 20, List.of("Short", "Line two")));
        var renders = new AtomicInteger();
        var renderer = new EvoPlusWidget(category, "render", "Test", "Description", false, true, available::get,
                () -> { renders.incrementAndGet(); return value.get(); });
        var editing = new AtomicBoolean(false);
        List<Object[]> draws = new ArrayList<>();
        RenderContext context = proxy(RenderContext.class, (self, method, values) -> switch (method.getName()) {
            case "isEditing" -> editing.get();
            case "lineHeight" -> 9;
            case "textWidth" -> ((String) values[0]).replaceAll("§.", "").length() * 6;
            case "text", "texture" -> { draws.add(values); yield null; }
            default -> throw new AssertionError("Renderer uses unsupported or absolute API: " + method);
        });
        renderer.render(context);
        check(draws.isEmpty() && renders.get() == 0, "Inactive widgets do not render or read game data");
        editing.set(true);
        renderer.render(context);
        check(draws.size() == 3, "Editor preview draws icon and both lines even without an active goal");
        check((double) draws.getFirst()[1] == 0, "Icon drawn relative to widget origin");
        check((double) draws.get(1)[1] == 23 && (double) draws.get(1)[2] == 1.5, "Text is centered in the widget bounds");
        check((double) draws.get(2)[2] == 11.5, "Lines do not overlap");
        check((int) draws.get(1)[3] == 0xFFFFFFFF, "Text remains opaque");
        var state = rendererSettings.widgets.get("render");
        check(state.width == 73 && state.height == 22, "Editor bounds contain icon and text");
        draws.clear(); editing.set(false); available.set(true);
        value.set(new EvoPlusWidget.Content("minecraft:textures/item/nether_star.png", 24, List.of("A much longer counter", "Two", "Three")));
        renderer.render(context);
        check(draws.size() == 4 && state.width > 73 && state.height == 31, "Live data resizes bounds without overriding position or scale");
        check(state.enabledWrites == 0, "Rendering does not change visibility");
        draws.clear();
        value.set(new EvoPlusWidget.Content("minecraft:textures/item/diamond_sword.png", 16, List.of("Kills: 135")));
        renderer.render(context);
        check(state.height == 18 && (double) draws.get(1)[2] == 4.5,
                "Single-line kill counter is vertically centered against the sword");
        draws.clear();
        var fishingGoal = FishingGoals.Goal.level(FishingData.World.END, FishingData.Rarity.LEGENDARY, 18, 19, 663552);
        var fishingContent = EvoPlusIntegration.fishingContent(fishingGoal, 15000);
        check(fishingContent.lines().size() == 5 && fishingContent.lines().get(3).contains("§b15000§f / 155648")
                && fishingContent.lines().get(4).contains("§6140648§f"), "Fishing widget colors full inventory experience and remainder");
        check(fishingContent.lines().getFirst().contains("§6Legendary§f")
                && fishingContent.lines().get(1).contains("§dEnd§f")
                && fishingContent.lines().get(2).contains("§dlevel 19§f"), "Pet rarity, world and full target level have their own colors");
        value.set(fishingContent);
        renderer.render(context);
        check(draws.size() == 6 && state.height == 51, "Fishing bounds include the fish and all five rows");
        check((double) draws.getFirst()[2] + 12 == state.height / 2,
                "The fish icon is centered vertically beside the third text row");
        var customFishing = EvoPlusIntegration.fishingContent(FishingGoals.Goal.custom(FishingData.World.OVERWORLD, 40000), 15009);
        check(customFishing.lines().size() == 3 && customFishing.lines().getFirst().contains("§aOverworld§f")
                && customFishing.lines().get(1).contains("§b15009§f / 40000")
                && customFishing.lines().get(2).contains("§624991§f"),
                "Experience goal shows only world, inventory progress and remaining experience");
        value.set(customFishing); draws.clear(); renderer.render(context);
        check(draws.size() == 4 && state.height == 31 && (double) draws.getFirst()[2] + 12 == state.height / 2,
                "The three-row experience widget keeps its fish centered");
        for (var world : FishingData.World.values()) {
            String color = switch (world) { case OVERWORLD -> "§a"; case NETHER -> "§c"; case END -> "§d"; };
            var colored = EvoPlusIntegration.fishingContent(FishingGoals.Goal.custom(world, 100), 0);
            check(colored.lines().getFirst().contains(color + Texts.tr(world.label()) + "§f"), "World color in experience goal: " + world);
        }
        for (var rarity : FishingData.Rarity.values()) {
            String color = switch (rarity) { case COMMON -> "§a"; case RARE -> "§e"; case LEGENDARY -> "§6"; };
            var colored = EvoPlusIntegration.fishingContent(FishingGoals.Goal.level(FishingData.World.OVERWORLD, rarity, 1, 2, 0), 0);
            check(colored.lines().getFirst().contains(color + Texts.tr(rarity.label()) + "§f"), "Pet rarity color: " + rarity);
        }
        check(ResetDetails.amount(new java.math.BigDecimal("12345678901234567890.125"))
                .equals("12 345 678 901 234 567 890.125"), "Reset dialogs retain exact money without abbreviation or rounding");

        ConfigVisual.goalNoticeDurationSeconds = 17;
        var notification = EvoPlusIntegration.goalNotification("Goal reached");
        check(notification.getTitle().equals("EvoAssist") && notification.getMessage().equals("Goal reached"), "Notification content preserved");
        check(notification.getDuration().toSeconds() == 17, "Configured notification duration preserved");
        ConfigVisual.goalNoticeDurationSeconds = 0;
        check(EvoPlusIntegration.goalNotification("Goal").getDuration().toSeconds() == 1, "Notification duration lower bound");
        ConfigVisual.goalNoticeDurationSeconds = 100;
        check(EvoPlusIntegration.goalNotification("Goal").getDuration().toSeconds() == 60, "Notification duration upper bound");
        System.out.println("EvoPlus integration checks passed: " + assertions);
    }
}
