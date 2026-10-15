package com.konzy.evo_assist.client.features.rewards;

import java.util.Optional;
import java.util.regex.Pattern;

/** Recognizes the server's personal kill confirmation, never player chat quoting it. */
public final class PlayerKillParser {
    private static final Pattern KILL = Pattern.compile(
            "^Вы\\s+убили\\s+игрока\\s+([A-Za-z0-9_]{1,16})\\s+и\\s+получили\\s+"
                    + "\\d+(?:[ \\u00a0\\u202f]\\d{3})*(?:[.,]\\d+)?[KMBTQ]?\\s*\\$[.!]?$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private PlayerKillParser() {}

    public static Optional<String> parse(String line) {
        String clean = line.replaceAll("§.", "").replace('\u00a0', ' ').replace('\u202f', ' ').strip();
        var match = KILL.matcher(clean);
        return match.matches() ? Optional.of(match.group(1)) : Optional.empty();
    }
}
