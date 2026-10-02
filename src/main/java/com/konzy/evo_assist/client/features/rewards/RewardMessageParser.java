package com.konzy.evo_assist.client.features.rewards;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RewardMessageParser {
    public enum Type { MONEY, SHARDS, TOKENS, CLAN_POINTS, CLAN_EXPERIENCE, CLAN_GOLD }
    public record Reward(Type type, BigDecimal amount) {}
    private static final Pattern LINE = Pattern.compile(
            "^\\+\\s*(\\d+(?:[ \\u00a0\\u202f]\\d{3})*(?:[.,]\\d+)?)([KMBTQ]?)\\s*(.*?)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BONUS = Pattern.compile("\\s*\\([^)]*бонус[^)]*\\)\\s*$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final String[] SUFFIXES = { "", "K", "M", "B", "T", "Q" };

    private RewardMessageParser() {}

    public static Optional<Reward> parse(String line) {
        String clean = line.replaceAll("§.", "").replace('\u00a0', ' ').replace('\u202f', ' ').strip();
        Matcher match = LINE.matcher(clean);
        if (!match.matches()) return Optional.empty();
        String suffix = match.group(2).toUpperCase(Locale.ROOT);
        String label = BONUS.matcher(match.group(3)).replaceFirst("").strip().toLowerCase(Locale.ROOT);
        Type type;
        if (label.matches("очк(?:о|а|ов) клана")) type = Type.CLAN_POINTS;
        else if (label.matches("опыт(?:а)? клана")) type = Type.CLAN_EXPERIENCE;
        else if (label.matches("золот(?:о|а) клана")) type = Type.CLAN_GOLD;
        else if (label.matches("жетон(?:а|ов)?")) type = Type.TOKENS;
        else if (label.matches("шард(?:а|ов|ы)?") || label.equals("\uE365")) type = Type.SHARDS;
        else if (label.equals("\uE135") || label.equals("$") || label.matches("ден(?:ьги|ег)")
                || label.isEmpty()) type = Type.MONEY;
        else return Optional.empty();
        // Only money has a compact suffix; reward counts are whole values.
        if (type != Type.MONEY && !suffix.isEmpty()) return Optional.empty();
        BigDecimal amount = new BigDecimal(match.group(1).replaceAll("[ \\u00a0\\u202f]", "").replace(',', '.'));
        int exponent = switch (suffix) {
            case "K" -> 3; case "M" -> 6; case "B" -> 9; case "T" -> 12; case "Q" -> 15; default -> 0;
        };
        amount = amount.scaleByPowerOfTen(exponent);
        if (type != Type.MONEY && amount.stripTrailingZeros().scale() > 0) return Optional.empty();
        return Optional.of(new Reward(type, amount));
    }

    public static String compactMoney(BigDecimal amount) {
        int group = 0;
        BigDecimal scaled = amount;
        while (scaled.abs().compareTo(BigDecimal.valueOf(1000)) >= 0 && group < SUFFIXES.length - 1) {
            scaled = scaled.movePointLeft(3);
            group++;
        }
        scaled = scaled.setScale(2, RoundingMode.HALF_UP);
        if (scaled.abs().compareTo(BigDecimal.valueOf(1000)) >= 0 && group < SUFFIXES.length - 1) {
            scaled = scaled.movePointLeft(3).setScale(2, RoundingMode.HALF_UP);
            group++;
        }
        return scaled.stripTrailingZeros().toPlainString() + SUFFIXES[group];
    }

    public static String whole(long amount) {
        return String.format(Locale.ROOT, "%,d", amount).replace(',', ' ');
    }
}
