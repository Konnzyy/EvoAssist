package com.konzy.evo_assist.client.features.fishing;

import com.konzy.evo_assist.client.features.fishing.FishingData.Rarity;
import com.konzy.evo_assist.client.features.fishing.FishingData.World;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;

/** An active fishing goal is saved per server; progress always reflects current inventory contents. */
public final class FishingGoals {
    public enum Mode { LEVEL, CUSTOM }
    public record Goal(Mode mode, World world, Rarity rarity, int currentLevel, int targetLevel,
                       long currentExperience, long targetExperience) {
        public Goal {
            Objects.requireNonNull(mode); Objects.requireNonNull(world); Objects.requireNonNull(rarity);
            if (targetExperience <= 0) throw new IllegalArgumentException("Empty fishing target");
        }
        public static Goal level(World world, Rarity rarity, int current, int target, long experience) {
            return new Goal(Mode.LEVEL, world, rarity, current, target, experience,
                    FishingData.requiredExperience(rarity, current, target, experience));
        }
        public static Goal custom(World world, long experience) {
            return new Goal(Mode.CUSTOM, world, Rarity.COMMON, 0, 0, 0, experience);
        }
    }
    private static final class State {
        final Goal goal;
        boolean notified;
        State(Goal goal, boolean notified) { this.goal = goal; this.notified = notified; }
    }
    private final Path file;
    private final Consumer<Goal> completed;
    private final Consumer<Exception> errors;
    private final Map<String, State> servers = new HashMap<>();
    private final EnumMap<World, Long> inventory = new EnumMap<>(World.class);
    private String server = "";
    private boolean ready;

    public FishingGoals(Path file, Consumer<Goal> completed, Consumer<Exception> errors) {
        this.file = file; this.completed = completed; this.errors = errors;
        load();
    }
    public void connect(String address) {
        server = address == null ? "" : address.strip().toLowerCase(Locale.ROOT);
        inventory.clear(); ready = false;
    }
    public void disconnect() { server = ""; inventory.clear(); ready = false; }
    public boolean connected() { return !server.isEmpty(); }
    public boolean ready() { return ready; }
    public Goal goal() { State state = servers.get(server); return connected() && state != null ? state.goal : null; }
    public long experience(World world) { return inventory.getOrDefault(world, 0L); }
    public long progress() { return goal() == null ? 0 : experience(goal().world()); }
    public long remaining() { return goal() == null ? 0 : Math.max(0, goal().targetExperience() - progress()); }
    public boolean complete() { return ready && goal() != null && remaining() == 0; }

    public void setGoal(Goal goal) {
        if (!connected()) throw new IllegalStateException("Join a world first");
        State previous = servers.get(server);
        if (previous != null && previous.goal.equals(goal)) return;
        servers.put(server, new State(goal, false));
        save();
        checkCompletion();
    }
    public void reset() {
        if (connected() && servers.remove(server) != null) save();
    }
    public void updateInventory(Map<World, Long> values) {
        if (!connected()) return;
        inventory.clear();
        for (World world : World.values()) inventory.put(world, Math.max(0, values.getOrDefault(world, 0L)));
        ready = true;
        checkCompletion();
    }
    private void checkCompletion() {
        State state = servers.get(server);
        if (!complete() || state.notified) return;
        state.notified = true;
        save();
        completed.accept(state.goal);
    }

    private void save() {
        var values = new Properties();
        for (var entry : servers.entrySet()) {
            String prefix = "server." + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(entry.getKey().getBytes(StandardCharsets.UTF_8)) + ".";
            State state = entry.getValue(); Goal goal = state.goal;
            values.setProperty(prefix + "address", entry.getKey());
            values.setProperty(prefix + "mode", goal.mode().name());
            values.setProperty(prefix + "world", goal.world().name());
            values.setProperty(prefix + "rarity", goal.rarity().name());
            values.setProperty(prefix + "currentLevel", Integer.toString(goal.currentLevel()));
            values.setProperty(prefix + "targetLevel", Integer.toString(goal.targetLevel()));
            values.setProperty(prefix + "currentExperience", Long.toString(goal.currentExperience()));
            values.setProperty(prefix + "targetExperience", Long.toString(goal.targetExperience()));
            values.setProperty(prefix + "notified", Boolean.toString(state.notified));
        }
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try (var out = Files.newOutputStream(temporary)) { values.store(out, "EvoAssist fishing goals by server"); }
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException | IllegalArgumentException error) { errors.accept(error); }
    }
    private void load() {
        if (!Files.isRegularFile(file)) return;
        var values = new Properties();
        try (var in = Files.newInputStream(file)) {
            values.load(in);
            for (String key : values.stringPropertyNames()) {
                if (!key.startsWith("server.") || !key.endsWith(".address")) continue;
                String prefix = key.substring(0, key.length() - "address".length());
                try {
                    String address = values.getProperty(key).strip().toLowerCase(Locale.ROOT);
                    if (address.isEmpty()) continue;
                    World world = World.valueOf(values.getProperty(prefix + "world"));
                    Mode mode = Mode.valueOf(values.getProperty(prefix + "mode"));
                    Goal goal = mode == Mode.CUSTOM ? Goal.custom(world, Long.parseLong(values.getProperty(prefix + "targetExperience")))
                            : Goal.level(world, Rarity.valueOf(values.getProperty(prefix + "rarity")),
                            Integer.parseInt(values.getProperty(prefix + "currentLevel")),
                            Integer.parseInt(values.getProperty(prefix + "targetLevel")),
                            Long.parseLong(values.getProperty(prefix + "currentExperience")));
                    servers.put(address, new State(goal, Boolean.parseBoolean(values.getProperty(prefix + "notified", "false"))));
                } catch (IllegalArgumentException | NullPointerException error) { errors.accept(error); }
            }
        } catch (IOException error) { errors.accept(error); }
    }
}
