/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.features.mine.blockPH;


import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import com.konzy.evo_assist.client.util.MoneyUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockProfitPerHour {

    // Config
    private static final long AUTO_PAUSE_AFTER_MS = 5_000;
    private final MiningStatistics statistics;
    private final ConfirmedBlockRewards confirmedRewards = new ConfirmedBlockRewards(this::confirmedBlock);
    private final ServerConfirmedBlockBreaks confirmedBreaks = new ServerConfirmedBlockBreaks(this::confirmedBreak);
    
    public long totalBrokenBlocks = 0;
    public long uptime = 0;
    public long totalMoney = 0;
    public long totalShards = 0;
    private long lastBlockBreakAt = -1;
    public boolean paused = true;

    public long BlocksPerHour = 0;
    public long MoneyPerHour = 0;
    public long ShardsPerHour = 0;


    private static final Pattern MONEY_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?[KMBTQ])", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHARD_MULTIPLIER_PATTERN = Pattern.compile("(\\d+)");

    public BlockProfitPerHour(MiningStatistics statistics) {
        this.statistics = statistics;
        refreshStatistics();
    }

    public void connect(String address) {
        clearActivity();
        confirmedBreaks.clear();
        statistics.connect(address);
        refreshStatistics();
    }

    public void disconnect() {
        confirmedBreaks.clear();
        statistics.disconnect();
        clearActivity();
        refreshStatistics();
    }

    private void confirmedBlock(long reward) {
        long money = countBlockMoney() ? reward : 0;
        if (money > 0) {
            record(0, money, 0);
            MiningGoals.getInstance().moneyEarned(money);
        }
    }

    private void confirmedBreak() {
        lastBlockBreakAt = System.currentTimeMillis();
        paused = false;
        record(1, 0, 0);
        MiningGoals.getInstance().blockBroken();
    }

    public void localBlockBreak(BlockPos pos) {
        if (!statistics.isConnected()) return;
        confirmedBreaks.localBreak(pos);
    }

    public void serverBlockUpdate(BlockPos pos, BlockState state) {
        if (!statistics.isConnected()) return;
        confirmedBreaks.serverUpdate(pos, state.isAir());
    }

    public void expirePendingBreaks() {
        confirmedBreaks.expirePending();
    }

    public boolean hasStatistics() {
        return totalBrokenBlocks != 0 || uptime != 0 || totalMoney != 0 || totalShards != 0;
    }

    public void reset() {
        confirmedBreaks.clear();
        statistics.reset();
        clearActivity();
        refreshStatistics();
    }

    private void clearActivity() {
        lastBlockBreakAt = -1;
        paused = true;
    }
    public void updateActionBar(Component text) {
        if (!statistics.isConnected()) return;
        confirmedRewards.actionBar(text.getString());
    }

    private boolean countBlockMoney() {
        return Arrays.asList(ConfigMining.bphWidgetAllowed).contains(ConfigMining.bphAllowEnum.BLOCKS);
    }

    public void getMessage(Component text, boolean overlay) {
        if(overlay || !statistics.isConnected()) return;
        String msg = text.getString();

        if(msg.startsWith("Вы нашли шард!")) {
            if (msg.length() > 14) {
                Matcher matcher = SHARD_MULTIPLIER_PATTERN.matcher(msg);
                if (matcher.find(1)) {
                    int earned = Integer.parseInt(matcher.group(1));
                    record(0, 0, earned);
                    MiningGoals.getInstance().shardsEarned(earned);
                }
            } else {
                record(0, 0, 1);
                MiningGoals.getInstance().shardsEarned(1);
            }
        }


        boolean found = isFound(msg);
        if(found) {
            Matcher matchingText = MONEY_PATTERN.matcher(text.getString().replaceAll("§.", ""));
            if(matchingText.find()) {
                long price = MoneyUtils.convertFrom(matchingText.group(1));
                record(0, price, 0);
                MiningGoals.getInstance().moneyEarned(price);
            }
        }
    }

    private static boolean isFound(String msg) {
        boolean found = false;
        if(Arrays.asList(ConfigMining.bphWidgetAllowed).contains(ConfigMining.bphAllowEnum.BARRELS) && (msg.startsWith("Фиолетовая бочка") || msg.startsWith("Красная бочка"))) {
            found = true;

        } else
        if(Arrays.asList(ConfigMining.bphWidgetAllowed).contains(ConfigMining.bphAllowEnum.RUNES) && msg.startsWith("Руна")) {
            found = true;

        } else
        if(Arrays.asList(ConfigMining.bphWidgetAllowed).contains(ConfigMining.bphAllowEnum.BOMBS) && msg.startsWith("Вы взорвали")) {
            found = true;

        } else
        if(Arrays.asList(ConfigMining.bphWidgetAllowed).contains(ConfigMining.bphAllowEnum.PETS) && msg.startsWith("Вы сломали всю шахту")) {
            found = true;

        } else
        if(msg.startsWith("Вы сломали")) {
            if((Arrays.asList(ConfigMining.bphWidgetAllowed).contains(ConfigMining.bphAllowEnum.RUNES) && msg.contains("руны")) ||
                    Arrays.asList(ConfigMining.bphWidgetAllowed).contains(ConfigMining.bphAllowEnum.WANDS)) found = true;

        } else
        if(Arrays.asList(ConfigMining.bphWidgetAllowed).contains(ConfigMining.bphAllowEnum.MULTITOOL) && (
                msg.startsWith("Сокрушающий меч разрубил") ||
                msg.startsWith("Залп стрел") ||
                msg.startsWith("Пожиратель блоков прокатился") ||
                msg.startsWith("Чёрная дыра поглотила"))) {

            found = true;

        }
        return found;
    }

    public static BlockProfitPerHour getInstance() {
        return EvoAssistClient.eventBlockProfitPerHour;
    }

    public void second() {
        if(lastBlockBreakAt < 0 || System.currentTimeMillis() - lastBlockBreakAt >= AUTO_PAUSE_AFTER_MS) {
            paused = true;
        }
        if (!paused && statistics.isConnected()) {
            statistics.activeSecond();
            MiningGoals.getInstance().activeSecond();
        }
        statistics.tick();
        refreshStatistics();
    }

    private void record(long blocks, long money, long shards) {
        statistics.record(blocks, money, shards);
        refreshTotals();
    }

    private void refreshTotals() {
        var totals = statistics.current();
        totalBrokenBlocks = totals.blocks;
        totalMoney = totals.money;
        totalShards = totals.shards;
        uptime = totals.activeMillis;
    }

    private void refreshStatistics() {
        refreshTotals();
        var rates = statistics.rates();
        BlocksPerHour = rates.blocks();
        MoneyPerHour = rates.money();
        ShardsPerHour = rates.shards();
    }

}
