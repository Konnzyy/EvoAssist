package com.konzy.evo_assist.client.features.fishing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Server fish values and per-level pet costs supplied with the mod. */
public final class FishingData {
    public static final int MAX_LEVEL = 21;
    public enum World {
        OVERWORLD(1, 11), NETHER(11, 16), END(16, MAX_LEVEL);
        private final int firstLevel;
        private final int maxLevel;
        World(int firstLevel, int maxLevel) { this.firstLevel = firstLevel; this.maxLevel = maxLevel; }
        public int firstLevel() { return firstLevel; }
        public int maxLevel() { return maxLevel; }
        public String label() { return "evoassist.fishing.world." + name().toLowerCase(Locale.ROOT); }
    }
    public enum Rarity {
        COMMON, RARE, LEGENDARY;
        public String label() { return "evoassist.fishing.rarity." + name().toLowerCase(Locale.ROOT); }
    }
    public record Fish(World world, long experience) {}
    public record ItemExperience(World world, long experience) {}

    private static final Pattern COLOR = Pattern.compile("§.");
    private static final Pattern BUCKET_EXPERIENCE = Pattern.compile(
            "опыта?\\s+питомца\\s+в\\s+ведре\\s*:\\s*([0-9][0-9 ,._]*)\\s*/\\s*([0-9][0-9 ,._]*)");
    private static final Map<String, Fish> FISH = new HashMap<>();
    private static final long[][] LEVELS = new long[Rarity.values().length][MAX_LEVEL];

    static {
        try (var stream = Objects.requireNonNull(FishingData.class.getResourceAsStream(
                "/assets/evoassist/data/pet_experience_and_fish.csv"), "Missing fishing data");
             var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] row = line.split(",", -1);
                if (row.length != 7) throw new IOException("Invalid fishing data row");
                if (row[0].equals("Рыба")) {
                    World world = switch (row[1]) {
                        case "Обычный мир" -> World.OVERWORLD;
                        case "Ад" -> World.NETHER;
                        case "Энд" -> World.END;
                        default -> throw new IOException("Invalid fish world");
                    };
                    long experience = Long.parseLong(row[6]);
                    if (experience <= 0 || FISH.put(normalize(row[5]), new Fish(world, experience)) != null)
                        throw new IOException("Invalid or duplicate fish value");
                } else if (row[0].equals("Уровень питомца")) {
                    Rarity rarity = switch (row[2]) {
                        case "Обычный" -> Rarity.COMMON;
                        case "Редкий" -> Rarity.RARE;
                        case "Легендарный" -> Rarity.LEGENDARY;
                        default -> throw new IOException("Invalid pet rarity");
                    };
                    int level = Integer.parseInt(row[3]);
                    if (level < 1 || level > 15 || LEVELS[rarity.ordinal()][level] != 0)
                        throw new IOException("Invalid or duplicate pet level");
                    LEVELS[rarity.ordinal()][level] = Long.parseLong(row[4]);
                } else throw new IOException("Invalid fishing record type");
            }
            for (Rarity rarity : Rarity.values()) {
                for (int level = 1; level <= 15; level++) {
                    if (LEVELS[rarity.ordinal()][level] <= 0) throw new IOException("Missing pet level cost");
                }
                // End levels 16-20 repeat Nether costs 11-15. Level 21 has no next level.
                for (int level = 16; level < MAX_LEVEL; level++)
                    LEVELS[rarity.ordinal()][level] = LEVELS[rarity.ordinal()][level - 5];
            }
        } catch (IOException | RuntimeException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    private FishingData() {}

    public static long nextLevelExperience(Rarity rarity, int level) {
        if (level < 1 || level >= MAX_LEVEL) throw new IllegalArgumentException("No next pet level");
        return LEVELS[Objects.requireNonNull(rarity).ordinal()][level];
    }

    public static long requiredExperience(Rarity rarity, int currentLevel, int targetLevel, long currentExperience) {
        if (targetLevel <= currentLevel || targetLevel > MAX_LEVEL || currentExperience < 0
                || currentExperience >= nextLevelExperience(rarity, currentLevel))
            throw new IllegalArgumentException("Invalid pet progress or target level");
        // The selected target uses its own threshold, minus the pet's entered progress.
        return nextLevelExperience(rarity, targetLevel - 1) - currentExperience;
    }

    /** Name matching intentionally supports the server's Russian item names. */
    public static ItemExperience itemExperience(String name, int count, List<String> lore, boolean includeBuckets) {
        if (count <= 0) return null;
        String normalized = normalize(name);
        Fish fish = FISH.get(normalized);
        if (fish != null) return new ItemExperience(fish.world(), multiply(fish.experience(), count));
        if (!includeBuckets) return null;
        World bucket = switch (normalized.replaceAll("[!.]+$", "").strip()) {
            case "ведро для рыбы" -> World.OVERWORLD;
            case "ведро для адской рыбы" -> World.NETHER;
            case "ведро для эндер рыбы", "ведро для эндер-рыбы", "ведро для эндской рыбы" -> World.END;
            default -> null;
        };
        if (bucket == null) return null;
        for (String line : lore) {
            var match = BUCKET_EXPERIENCE.matcher(normalize(line));
            if (!match.find()) continue;
            try {
                long experience = Long.parseLong(match.group(1).replaceAll("[^0-9]", ""));
                return new ItemExperience(bucket, multiply(experience, count));
            } catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private static long multiply(long value, int count) {
        return value > Long.MAX_VALUE / count ? Long.MAX_VALUE : value * count;
    }

    public static String normalize(String input) {
        String value = COLOR.matcher(input).replaceAll("");
        StringBuilder clean = new StringBuilder();
        value.codePoints().filter(cp -> Character.getType(cp) != Character.PRIVATE_USE
                && Character.getType(cp) != Character.FORMAT).forEach(clean::appendCodePoint);
        return clean.toString().replace('\u00a0', ' ').replace('\u202f', ' ')
                .toLowerCase(Locale.ROOT).replace('ё', 'е').strip().replaceAll("\\s+", " ");
    }
}
