/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.util;

public class MoneyUtils {
    public static long convertFrom(String msg) {
        try {
            return GoalAmount.parse(msg, false).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        } catch(Exception e) {
            return 0;
        }
    }

    public static String convertTo(long msg) {
        long length = length(msg);
        String money;

        if (length >= 1 && length <= 3) {
            money = String.format( "%s", msg);
        } else if (length >= 4 && length <= 6) {
            money = String.format( "%sK", (double)Math.round((float) msg / 1000 * 10)/10);
        } else if (length >= 7 && length <= 9) {
            money = String.format( "%sM", (double)Math.round((float) msg / 1000000* 10)/10);
        } else if (length >= 10 && length <= 12) {
            money = String.format( "%sB", (double)Math.round((float) msg / 1000000000L* 10)/10);
        } else if (length >= 13 && length <= 15) {
            money = String.format( "%sT", (double)Math.round((float) msg / 1000000000000L* 10)/10);
        } else if (length >= 16 && length <= 18) {
            money = String.format( "%sQ", (double)Math.round((float) msg / 1000000000000000L* 10)/10);
        } else {
            money = String.format( "%s", msg);
        }

        return money;
    }

    public static long length(long value) {
        if (value == 0L) {
            return 1;
        }
        return (long) (Math.log10(Math.abs((double) value))) + 1;
    }
}
