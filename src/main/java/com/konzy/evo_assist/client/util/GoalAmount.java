package com.konzy.evo_assist.client.util;

import static com.konzy.evo_assist.client.util.Texts.tr;
import com.konzy.evo_assist.client.features.rewards.RewardMessageParser;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

public final class GoalAmount {
    private static final Pattern VALUE = Pattern.compile("\\d+(?:\\.\\d+)?[KMBTQ]?", Pattern.CASE_INSENSITIVE);
    private GoalAmount() {}

    public static BigDecimal parse(String input, boolean whole) {
        String text = input.strip().toUpperCase(Locale.ROOT);
        if (text.length() > 32 || !VALUE.matcher(text).matches())
            throw new IllegalArgumentException(tr("evoassist.error.amount"));
        char last = text.charAt(text.length() - 1);
        int exponent = switch (last) {
            case 'K' -> 3; case 'M' -> 6; case 'B' -> 9; case 'T' -> 12; case 'Q' -> 15; default -> 0;
        };
        BigDecimal amount = new BigDecimal(exponent == 0 ? text : text.substring(0, text.length() - 1))
                .scaleByPowerOfTen(exponent).stripTrailingZeros();
        if (amount.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0)
            throw new IllegalArgumentException(tr("evoassist.error.largeGoal"));
        if (whole && amount.scale() > 0)
            throw new IllegalArgumentException(tr("evoassist.error.wholeGoal"));
        return amount;
    }

    public static String format(BigDecimal amount) { return RewardMessageParser.compactMoney(amount); }
}
