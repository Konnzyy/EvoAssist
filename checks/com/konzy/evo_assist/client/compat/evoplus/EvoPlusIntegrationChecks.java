package com.konzy.evo_assist.client.compat.evoplus;

import com.konzy.evo_assist.client.config.ConfigBosses;
import com.konzy.evo_assist.client.config.ConfigChat;
import com.konzy.evo_assist.client.config.ConfigClan;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.config.ConfigVisual;
import com.konzy.evo_assist.client.util.Texts;
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
import ru.dargen.evoplus.api.addon.Addon;
import ru.dargen.evoplus.api.render.RenderContext;
import ru.dargen.evoplus.api.render.WidgetRenderer;
import ru.dargen.evoplus.api.setting.AddonSettings;
import ru.dargen.evoplus.api.setting.SettingCategory;
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
        WidgetRenderer renderer;
        final WidgetElement element = proxy(WidgetElement.class, (self, method, args) -> switch (method.getName()) {
            case "description" -> { description = (String) args[0]; yield self; }
            case "size" -> { width = (double) args[0]; height = (double) args[1]; yield self; }
            case "isEnabled" -> enabled;
            case "setEnabled" -> { enabled = (boolean) args[0]; enabledWrites++; yield null; }
            case "getWidth" -> width;
            case "getHeight" -> height;
            default -> throw new AssertionError("Unexpected widget API: " + method);
        });
    }

    private static final class SettingsState {
        final Map<String, WidgetState> widgets = new LinkedHashMap<>();
        final Map<String, String> categories = new LinkedHashMap<>();
        Path file;
        int saves, opens;
        boolean savedVisibility;
        final AddonSettings settings = proxy(AddonSettings.class, (self, method, args) -> switch (method.getName()) {
            case "getFile" -> file;
            case "save" -> { saves++; yield null; }
            case "open" -> { opens++; yield null; }
            case "category" -> {
                String id = (String) args[0];
                check(categories.put(id, (String) args[1]) == null, "Unique category " + id);
                yield proxy(SettingCategory.class, (container, operation, values) -> {
                    if (!operation.getName().equals("widget")) throw new AssertionError(operation);
                    String widgetId = (String) values[0];
                    var state = new WidgetState();
                    state.enabled = savedVisibility;
                    state.name = (String) values[1];
                    state.width = (double) values[2]; state.height = (double) values[3];
                    state.renderer = (WidgetRenderer) values[4];
                    check(widgets.put(widgetId, state) == null, "Unique widget " + widgetId);
                    return state.element;
                });
            }
            default -> throw new AssertionError("Unexpected settings API: " + method);
        });
        final Addon addon = proxy(Addon.class, (self, method, args) -> {
            if (method.getName().equals("getSettings")) return settings;
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
                "clan_gold_goal", "clan_experience_goal", "chat_tabs", "player_kills")), "Every existing HUD widget registered");
        check(first.categories.size() == 7, "Seven localized categories");
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
            case "textWidth" -> ((String) values[0]).length() * 6;
            case "text", "texture" -> { draws.add(values); yield null; }
            default -> throw new AssertionError("Renderer uses unsupported or absolute API: " + method);
        });
        renderer.render(context);
        check(draws.isEmpty() && renders.get() == 0, "Inactive widgets do not render or read game data");
        editing.set(true);
        renderer.render(context);
        check(draws.size() == 3, "Editor preview draws icon and both lines even without an active goal");
        check((double) draws.getFirst()[1] == 0, "Icon drawn relative to widget origin");
        check((double) draws.get(1)[1] == 23 && (double) draws.get(1)[2] == 1, "Text uses same widget coordinates");
        check((double) draws.get(2)[2] == 11, "Lines do not overlap");
        check((int) draws.get(1)[3] == 0xFFFFFFFF, "Text remains opaque");
        var state = rendererSettings.widgets.get("render");
        check(state.width == 73 && state.height == 22, "Editor bounds contain icon and text");
        draws.clear(); editing.set(false); available.set(true);
        value.set(new EvoPlusWidget.Content("minecraft:textures/item/nether_star.png", 24, List.of("A much longer counter", "Two", "Three")));
        renderer.render(context);
        check(draws.size() == 4 && state.width > 73 && state.height == 32, "Live data resizes bounds without overriding position or scale");
        check(state.enabledWrites == 0, "Rendering does not change visibility");

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
