package com.konzy.evo_assist.client.features.rewards;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Files;
import static com.konzy.evo_assist.client.features.rewards.RewardMessageParser.Type.*;

public final class RewardStatisticsChecks {
    private static int checks;
    private static void check(boolean success, String message) {
        checks++;
        if (!success) throw new AssertionError(message);
    }
    private static void parsed(String text, RewardMessageParser.Type type, String amount) {
        var reward = RewardMessageParser.parse(text).orElseThrow();
        check(reward.type() == type && reward.amount().compareTo(new BigDecimal(amount)) == 0, text);
    }
    public static void main(String[] args) throws Exception {
        parsed("§7+ 3.64B(84% бонус)", MONEY, "3640000000");
        parsed("+ 48 \uE365", SHARDS, "48");
        parsed("+ 1 жетон", TOKENS, "1");
        parsed("+ 38 очков клана", CLAN_POINTS, "38");
        parsed("+ 4 опыта клана", CLAN_EXPERIENCE, "4");
        parsed("+ 5 золота клана(37% Бонус)", CLAN_GOLD, "5");
        parsed("6 золота клана", CLAN_GOLD, "6");
        parsed("3 опыта клана", CLAN_EXPERIENCE, "3");
        parsed("+ 2.15M", MONEY, "2150000");
        parsed("+ 4.43T", MONEY, "4430000000000");
        parsed("+ 5.23Q", MONEY, "5230000000000000");
        parsed("+ 1K", MONEY, "1000");
        parsed("+ 250", MONEY, "250");
        parsed("+\u00a01\u202f234 очка клана", CLAN_POINTS, "1234");
        parsed("+ 2,15M", MONEY, "2150000");
        for (String text : new String[]{"Игрок: + 3.64B", "+ 1 меч", "- 5 золота клана", "6 очков клана", "250",
                "+ 1.5 жетона", "+ 5K очков клана", "Награды за босса", "+ 5 опыта персонажа"}) {
            check(RewardMessageParser.parse(text).isEmpty(), "Reject " + text);
        }
        check(RewardMessageParser.compactMoney(new BigDecimal("5230000000000000")).equals("5.23Q"), "Q formatting");
        check(RewardMessageParser.compactMoney(new BigDecimal("2150000")).equals("2.15M"), "two decimals");
        check(RewardMessageParser.compactMoney(new BigDecimal("1000")).equals("1K"), "no trailing zero");
        check(RewardMessageParser.compactMoney(new BigDecimal("999999")).equals("1M"), "rounded boundary");
        Path directory = Files.createDirectories(Path.of(args[0]));
        Path file = Files.createTempFile(directory, "reward-check-", ".properties");
        var stats = new RewardStatistics(file, error -> { throw new AssertionError(error); });
        check(!stats.hasBossStatistics() && !stats.hasClanStatistics(), "No reset data without a server");
        stats.connect("PLAY.EXAMPLE.COM:25565");
        check(!stats.hasBossStatistics() && !stats.hasClanStatistics(), "Empty server has no reset data");
        stats.receive("+ 3.64B(84% бонус)\n+ 48 \uE365\n+ 1 жетон\n+ 38 очков клана\n+ 4 опыта клана\n+ 5 золота клана(37% бонус)");
        check(stats.current().money.compareTo(new BigDecimal("3640000000")) == 0
                && stats.current().shards == 48 && stats.current().tokens == 1
                && stats.current().clanPoints == 38 && stats.current().clanExperience == 4
                && stats.current().clanGold == 5, "full screenshot batch");
        stats.receive("6 золота клана\n3 опыта клана");
        check(stats.current().clanGold == 11 && stats.current().clanExperience == 7,
                "Separate clan rewards without plus are accumulated");
        check(stats.hasBossStatistics() && stats.hasClanStatistics(), "Rewards enable both resets");
        stats.disconnect();
        stats.receive("+ 100Q");
        stats = new RewardStatistics(file, error -> { throw new AssertionError(error); });
        check(stats.current().money.compareTo(new BigDecimal("3640000000")) == 0, "saved on restart, disconnected messages ignored");
        stats.connect("other.example.com");
        check(!stats.hasBossStatistics() && !stats.hasClanStatistics(), "Reset availability follows selected server");
        check(stats.current().tokens == 0 && stats.current().clanPoints == 0, "separate server");
        stats.receive("+ 2 жетона");
        check(stats.hasBossStatistics() && !stats.hasClanStatistics(), "Tokens alone enable only boss reset");
        stats.connect("play.example.com");
        check(stats.current().tokens == 1 && stats.current().clanGold == 11, "restore original server and default port normalization");
        stats.resetBosses();
        check(!stats.hasBossStatistics() && stats.hasClanStatistics(), "Boss reset disables only its own button");
        check(stats.current().money.signum() == 0 && stats.current().shards == 0
                && stats.current().tokens == 0 && stats.current().clanPoints == 38, "independent boss reset");
        stats.receive("+ 1K");
        stats.resetClan();
        stats = new RewardStatistics(file, error -> { throw new AssertionError(error); });
        check(stats.current().clanPoints == 0 && stats.current().clanExperience == 0
                && stats.current().clanGold == 0 && stats.current().money.compareTo(new BigDecimal("1000")) == 0, "independent clan reset persists");
        check(stats.hasBossStatistics() && !stats.hasClanStatistics(), "Reset availability restored from saved data");
        stats.connect("other.example.com");
        check(stats.current().tokens == 2, "other server survives resets");
        Files.delete(file);
        System.out.println("Reward statistics checks passed: " + checks);
    }
}
