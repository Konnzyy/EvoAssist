package com.konzy.evo_assist.client.features.calculator;

import com.konzy.evo_assist.client.util.GoalAmount;
import com.konzy.evo_assist.client.features.rewards.RewardMessageParser;
import java.math.BigDecimal;
import java.util.List;

public final class LevelCalculatorChecks {
    private static int assertions;
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static void result(int from, int to, int discount, String money, long blocks) {
        var value = LevelCalculator.calculate(from, to, discount);
        check(value.money().compareTo(new BigDecimal(money)) == 0, "Exact money for " + from + " -> " + to);
        check(value.blocks() == blocks, "Target threshold, not remaining blocks");
        check(value.levels() == to - from, "Level difference");
    }
    private static void invalid(int from, int to, int discount) {
        try { LevelCalculator.calculate(from, to, discount); }
        catch (IllegalArgumentException expected) { assertions++; return; }
        throw new AssertionError("Accepted invalid input " + from + "/" + to + "/" + discount);
    }
    public static void main(String[] args) {
        result(1, 2, 0, "80", 10);
        result(324, 432, 0, "70971000000000", 1190000);
        result(500, 523, 10, "150390000000000", 3120000);
        result(543, 560, 20, "211040000000000", 4550000);
        result(481, 482, 5, "3990000000000", 2060000);
        result(481, 482, 10, "3780000000000", 2060000);
        result(500, 523, 0, "167100000000000", 3120000);
        check(LevelCalculator.discounts(481).equals(List.of(0)), "No discounted transitions through target 481");
        check(LevelCalculator.discounts(482).equals(List.of(0, 5, 10)), "First two discounts offered for target 482");
        check(LevelCalculator.discounts(540).equals(List.of(0, 5, 10)), "15/20 not offered early");
        check(LevelCalculator.discounts(541).equals(List.of(0, 5, 10, 15, 20)), "Dropdown gains 15/20 at 541");
        check(LevelCalculator.discounts(0).equals(List.of(0)), "Blank/invalid input offers no automatic discount");
        var later = LevelCalculator.calculate(540, 541, 10);
        check(later.money().compareTo(new BigDecimal("10620000000000")) == 0, "Purchased 10 percent remains usable at later levels");
        check(LevelCalculator.calculate(400, 550, 20).money().compareTo(LevelCalculator.calculate(400, 550, 0).money()) < 0,
                "Target 550 allows 20 percent even when starting at 400");
        check(LevelCalculator.calculate(400, 482, 10).money().compareTo(
                LevelCalculator.calculate(400, 482, 0).money().subtract(new BigDecimal("420000000000"))) == 0,
                "Discount begins at eligible transition, earlier transitions remain full price");
        var evo = LevelCalculator.calculate(400, 600, 0, LevelCalculator.Mode.EVO);
        var fast = LevelCalculator.calculate(400, 600, 0, LevelCalculator.Mode.EVO_FAST);
        check(evo.blocks() == 6980000 && fast.blocks() == 4653334, "Fast block threshold is divided by 1.5 and rounded up");
        check(fast.money().compareTo(evo.money()) == 0 && fast.levels() == evo.levels(), "Fast changes only blocks");
        check(GoalAmount.format(fast.money()).equals("2.44Q"), "Evo Fast screenshot money example");
        check(LevelCalculator.calculate(500, 523, 10, LevelCalculator.Mode.EVO_FAST).blocks() == 2080000,
                "Exact Fast block division");
        check(LevelCalculator.calculate(1, 2, 0, LevelCalculator.Mode.EVO_FAST).blocks() == 7,
                "Small thresholds never round below requirement");
        check(GoalAmount.format(LevelCalculator.calculate(500, 523, 10).money()).equals("150.39T"), "Compact money");
        check(RewardMessageParser.whole(LevelCalculator.calculate(500, 523, 10).blocks()).equals("3 120 000"), "Unabridged blocks");
        for (int level = 2; level <= 600; level++) {
            var single = LevelCalculator.calculate(level - 1, level, 0);
            check(single.money().compareTo(LevelCalculator.level(level).money()) == 0, "Inclusive target, exclusive current " + level);
        }
        invalid(0, 600, 0); invalid(600, 601, 0); invalid(500, 500, 0); invalid(500, 499, 0);
        invalid(400, 481, 5); invalid(400, 540, 15); invalid(541, 560, 17);
        System.out.println("Level calculator checks passed: " + assertions);
    }
}
