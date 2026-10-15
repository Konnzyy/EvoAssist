package com.konzy.evo_assist.client.features.rewards;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PlayerKillChecks {
    private static int assertions;
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        String message = "Вы убили игрока Heavencon24 и получили 18.62M$";
        check(PlayerKillParser.parse(message).orElseThrow().equals("Heavencon24"), "Victim parsed from exact server format");
        check(PlayerKillParser.parse("§aВы убили игрока §bPlayer_01 §aи получили §e18,62M$!").isPresent(), "Formatting and decimal comma");
        check(PlayerKillParser.parse("Вы убили игрока Alex и получили 1\u202f234$").isPresent(), "Grouped amounts");
        for (String rejected : new String[] {"Heavencon24: " + message, "[Клан] " + message,
                "Вас убил игрок Heavencon24", "Вы убили босса и получили 18.62M$", "Вы убили игрока Alex",
                "Вы убили игрока Alex и получили -18.62M$", "Вы убили игрока Alex и получили 18.62M$ за сообщение",
                "Вы убили игрока NameThatIsTooLong_17 и получили 1$"})
            check(PlayerKillParser.parse(rejected).isEmpty(), "Ignore " + rejected);

        Path directory = Files.createDirectories(Path.of(args[0]));
        Path file = Files.createTempFile(directory, "kills-", ".properties");
        var stats = new RewardStatistics(file, error -> { throw new AssertionError(error); },
                reward -> check(reward.type() == RewardMessageParser.Type.TOKENS, "Kill money never enters boss/goal callback"));
        stats.receive(message);
        check(!stats.hasKillStatistics() && stats.current().kills == 0, "Disconnected messages ignored");
        stats.connect("PLAY.EXAMPLE.COM:25565");
        stats.receive(message + "\n" + message + "\n+ 1 жетон");
        check(stats.current().kills == 2 && stats.hasKillStatistics(), "Repeated victim in real separate confirmations counts twice");
        check(stats.current().money.signum() == 0 && stats.current().tokens == 1, "Kills do not affect boss rewards");
        stats.disconnect();
        stats.receive(message);
        stats = new RewardStatistics(file, error -> { throw new AssertionError(error); });
        check(stats.current().kills == 2 && !stats.isConnected(), "Saved kills restored across restart");
        stats.connect("other.example.com");
        check(stats.current().kills == 0 && !stats.hasKillStatistics(), "Independent server counter");
        stats.receive(message);
        stats.connect("play.example.com");
        check(stats.current().kills == 2, "Default port normalized");
        stats.resetBosses(); stats.resetClan();
        check(stats.current().kills == 2, "Other resets preserve kills");
        stats.receive("+ 1 жетон");
        stats.resetKills();
        check(stats.current().kills == 0 && !stats.hasKillStatistics() && stats.current().tokens == 1, "Kill reset clears only kills");
        stats = new RewardStatistics(file, error -> { throw new AssertionError(error); });
        check(stats.current().kills == 0, "Reset persists");
        stats.connect("other.example.com");
        check(stats.current().kills == 1, "Reset preserves other servers");
        var legacy = new Properties();
        try (var in = Files.newInputStream(file)) { legacy.load(in); }
        legacy.stringPropertyNames().stream().filter(key -> key.endsWith(".kills")).toList().forEach(legacy::remove);
        try (var out = Files.newOutputStream(file)) { legacy.store(out, "Legacy format"); }
        stats = new RewardStatistics(file, error -> { throw new AssertionError(error); });
        stats.connect("play.example.com");
        check(stats.current().kills == 0 && stats.current().tokens == 1, "Old files gain zero kills without losing existing rewards");
        Files.delete(file);
        System.out.println("Player kill checks passed: " + assertions);
    }
}
