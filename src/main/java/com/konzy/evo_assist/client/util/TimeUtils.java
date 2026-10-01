package com.konzy.evo_assist.client.util;

import static com.konzy.evo_assist.client.util.Texts.tr;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {
    private static final Pattern TextTimePattern = Pattern.compile("(\\d*) (ч|мин|сек)");
    private static final Map<String, Long> TextTimeModifiers = Map.of(
            "ч", 3_600_000L,
            "мин", 60_000L,
            "сек", 1000L
    );

    public static String asShortTextTime(long millis) {
        if (millis < 1000L) return "00:00";

        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(":");
        }
        if (minutes > 0) {
            if (minutes < 10) sb.append("0");
            sb.append(minutes).append(":");
        } else {
            sb.append("00:");
        }
        if (seconds > 0) {
            if (seconds < 10) sb.append("0");
            sb.append(seconds);
        } else {
            sb.append("00");
        }
        return sb.toString();
    }

    public static String asTextTime(long millis) {
        if (millis < 1000L) return "0" + tr("evoassist.time.second");

        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds / 3600) % 24;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(tr("evoassist.time.day")).append(" ");
        }
        if (hours > 0) {
            sb.append(hours).append(tr("evoassist.time.hour")).append(" ");
        }
        if (minutes > 0) {
            sb.append(minutes).append(tr("evoassist.time.minute")).append(" ");
        }
        if (seconds > 0) {
            sb.append(seconds).append(tr("evoassist.time.second"));
        }
        return sb.toString().trim();
    }

    public static String asStrictTextTime(long millis) {
        if (millis < 1000L) return tr("evoassist.time.now");

        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(tr("evoassist.time.hour")).append(" ");
        }
        if (minutes > 0) {
            sb.append(minutes).append(tr("evoassist.time.minute")).append(" ");
        }
        if (hours == 0 && seconds > 0) {
            sb.append(seconds).append(tr("evoassist.time.second"));
        }
        return sb.toString();
    }

    public static long fromTextTime(String text) {
        Matcher matcher = TextTimePattern.matcher(text);
        long sum = 0;
        while (matcher.find()) {
            String time = matcher.group(1);
            String modifier = matcher.group(2);
            sum += parseTimeTextPart(time, modifier);
        }
        return sum;
    }

    private static long parseTimeTextPart(String time, String modifier) {
        long multiplier = TextTimeModifiers.getOrDefault(modifier, 0L);
        int timeValue;
        try {
            timeValue = Integer.parseInt(time);
        } catch (NumberFormatException e) {
            timeValue = 0;
        }
        return multiplier * timeValue;
    }
}
