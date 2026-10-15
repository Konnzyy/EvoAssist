package com.konzy.evo_assist.client.features.mine.blockPH;

import com.konzy.evo_assist.client.util.MoneyUtils;
import java.util.function.LongConsumer;
import java.util.regex.Pattern;

/** Parses the money amount from each received mining-reward action-bar update. */
public final class ConfirmedBlockRewards {
    private static final Pattern AMOUNT = Pattern.compile(
            "\\+\\s*(\\d+(?:[.,]\\d+)?[KMBTQ]?)(?![\\d.,\\p{L}%])", Pattern.CASE_INSENSITIVE);
    private final LongConsumer confirmed;

    public ConfirmedBlockRewards(LongConsumer confirmed) {
        this.confirmed = confirmed;
    }

    public void actionBar(String message) {
        long money = rewardAmount(message);
        // Identical packets are distinct rewards; never deduplicate them by text or time.
        if (money > 0) confirmed.accept(money);
    }

    public static long rewardAmount(String message) {
        if (message == null) return 0;
        String text = message.replaceAll("§.", "").strip();
        var match = AMOUNT.matcher(text);
        while (match.find()) {
            String after = text.substring(match.end()).stripLeading();
            // Currency icons identify mining rewards in compound action bars. A bare +amount
            // is the legacy format; percentages, XP and shard gains are not block money.
            if (after.startsWith("$") || after.startsWith("\uE135")
                    || (match.start() == 0 && after.isEmpty())) {
                return MoneyUtils.convertFrom(match.group(1).replace(',', '.'));
            }
        }
        return 0;
    }
}
