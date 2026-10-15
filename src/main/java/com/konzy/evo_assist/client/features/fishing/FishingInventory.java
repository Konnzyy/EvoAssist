package com.konzy.evo_assist.client.features.fishing;

import static com.konzy.evo_assist.client.util.Texts.tr;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.compat.evoplus.EvoPlusIntegration;
import com.konzy.evo_assist.client.config.ConfigFishing;
import com.konzy.evo_assist.client.features.fishing.FishingData.World;
import java.util.EnumMap;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** Reads server-supplied item names and lore without requiring an open inventory or tooltip. */
public final class FishingInventory {
    private static FishingGoals goals;
    private static int ticks;
    private FishingInventory() {}
    public static FishingGoals goals() {
        if (goals == null) goals = new FishingGoals(FabricLoader.getInstance().getConfigDir()
                .resolve("evoassist_fishing_goals.properties"),
                goal -> EvoPlusIntegration.notifyFishingGoal(tr("evoassist.fishing.complete", goal.targetExperience())),
                error -> EvoAssistClient.logger.warn("Could not update fishing goals", error));
        return goals;
    }
    public static void connect(String address) { ticks = 0; goals().connect(address); }
    public static void disconnect() { goals().disconnect(); }
    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null || !goals().connected() || goals().goal() == null) return;
        if (ticks++ % 4 == 0) refresh(client);
    }
    public static void refresh(Minecraft client) {
        if (client.player == null || client.level == null || !goals().connected()) return;
        var values = new EnumMap<World, Long>(World.class);
        var inventory = client.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) add(values, inventory.getItem(slot));
        // A stack carried by the mouse is no longer in its slot, but is still held by the player.
        add(values, client.player.containerMenu.getCarried());
        goals().updateInventory(values);
    }
    private static void add(EnumMap<World, Long> values, ItemStack stack) {
        if (stack.isEmpty()) return;
        var lore = stack.get(DataComponents.LORE);
        var experience = FishingData.itemExperience(stack.getHoverName().getString(), stack.getCount(),
                lore == null ? List.of() : lore.lines().stream().map(line -> line.getString()).toList(), ConfigFishing.includeBuckets);
        if (experience == null) return;
        long before = values.getOrDefault(experience.world(), 0L);
        values.put(experience.world(), experience.experience() > Long.MAX_VALUE - before
                ? Long.MAX_VALUE : before + experience.experience());
    }
}
