package com.konzy.evo_assist.client.features.calculator;

import static com.konzy.evo_assist.client.util.Texts.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Money is the cost of each transition; blocks are the cumulative server threshold. */
public final class LevelCalculator {
    public static final int MAX_LEVEL = 600;
    public enum Mode { EVO, EVO_FAST }
    public record Level(BigDecimal money, long blocks) {}
    public record Result(int levels, long blocks, BigDecimal money) {}
    private static final Level[] LEVELS = load();
    private LevelCalculator() {}

    public static boolean validLevel(int level) { return level >= 1 && level <= MAX_LEVEL; }

    public static List<Integer> discounts(int targetLevel) {
        if (!validLevel(targetLevel) || targetLevel < 482) return List.of(0);
        if (targetLevel < 541) return List.of(0, 5, 10);
        return List.of(0, 5, 10, 15, 20);
    }

    public static Level level(int level) {
        if (!validLevel(level)) throw new IllegalArgumentException(tr("evoassist.calculator.levelRange"));
        return LEVELS[level];
    }

    public static Result calculate(int currentLevel, int targetLevel, int discount) {
        return calculate(currentLevel, targetLevel, discount, Mode.EVO);
    }

    public static Result calculate(int currentLevel, int targetLevel, int discount, Mode mode) {
        if (!validLevel(currentLevel) || !validLevel(targetLevel))
            throw new IllegalArgumentException(tr("evoassist.calculator.levelRange"));
        if (targetLevel <= currentLevel)
            throw new IllegalArgumentException(tr("evoassist.calculator.targetHigher"));
        if (!discounts(targetLevel).contains(discount))
            throw new IllegalArgumentException(tr("evoassist.calculator.discountUnavailable"));
        BigDecimal money = BigDecimal.ZERO;
        BigDecimal multiplier = BigDecimal.valueOf(100 - discount, 2);
        for (int next = currentLevel + 1; next <= targetLevel; next++) {
            // Buying the first discount is possible at 481; it applies from level 482.
            boolean eligible = next >= (discount > 10 ? 541 : 482);
            money = money.add(LEVELS[next].money().multiply(eligible ? multiplier : BigDecimal.ONE));
        }
        long blocks = LEVELS[targetLevel].blocks();
        // Required blocks must be whole: round 2/3 upward so the target is reachable.
        if (mode == Mode.EVO_FAST) blocks = (blocks * 2 + 2) / 3;
        return new Result(targetLevel - currentLevel, blocks, money);
    }

    private static Level[] load() {
        var levels = new Level[MAX_LEVEL + 1];
        levels[1] = new Level(BigDecimal.ZERO, 0);
        var stream = LevelCalculator.class.getResourceAsStream("/assets/evoassist/data/levels.tsv");
        if (stream == null) throw new IllegalStateException("Missing level table");
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            if (!"level\tmoney\tblocks".equals(reader.readLine())) throw new IOException("Invalid level table header");
            for (int number = 2; number <= MAX_LEVEL; number++) {
                String line = reader.readLine();
                if (line == null) throw new IOException("Missing level " + number);
                String[] fields = line.split("\t");
                if (fields.length != 3 || Integer.parseInt(fields[0]) != number)
                    throw new IOException("Invalid level row " + number);
                var money = new BigDecimal(fields[1]);
                long blocks = Long.parseLong(fields[2]);
                if (money.signum() < 0 || blocks < levels[number - 1].blocks())
                    throw new IOException("Invalid level requirements " + number);
                levels[number] = new Level(money, blocks);
            }
            if (reader.readLine() != null) throw new IOException("Unexpected level table rows");
        } catch (IOException | NumberFormatException error) {
            throw new ExceptionInInitializerError(error);
        }
        return levels;
    }
}
