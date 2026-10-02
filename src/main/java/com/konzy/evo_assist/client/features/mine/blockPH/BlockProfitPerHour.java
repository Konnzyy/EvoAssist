/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.features.mine.blockPH;


import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.config.ConfigMining;
import com.konzy.evo_assist.client.features.mine.MiningGoals;
import com.konzy.evo_assist.client.features.goals.AdditionalGoals;
import com.konzy.evo_assist.client.util.MoneyUtils;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockProfitPerHour implements ClientPlayerBlockBreakEvents.After {

    // Config
    private static final long MAX_LATEST_ACTION_BAR_MS = 10_000;
    private static final long AUTO_PAUSE_AFTER_MS = 5_000;
    private final MiningStatistics statistics;
    
    public long totalBrokenBlocks = 0;
    public long uptime = 0;
    public long totalMoney = 0;
    public long totalShards = 0;
    private long lastBlockBreakAt = -1;
    private long lastActionBarAt = -1;
    private int blocksAwaitingPrice = 0;
    private long latestActionBar = 0;
    private long latestActionBarTimeout = 100;
    public boolean paused = true;

    public long BlocksPerHour = 0;
    public long MoneyPerHour = 0;
    public long ShardsPerHour = 0;


    static Pattern moneyPattern = Pattern.compile("(\\d+(?:\\.\\d+)?[KMBTQ])", Pattern.CASE_INSENSITIVE);
    static Pattern actionBarPattern = Pattern.compile("\\+(\\d+(?:\\.\\d+)?[KMBTQ]?)", Pattern.CASE_INSENSITIVE);
    static Pattern shardMultiplierPattern = Pattern.compile("(\\d+)");

    public BlockProfitPerHour(MiningStatistics statistics) {
        this.statistics = statistics;
        refreshStatistics();
    }

    public void connect(String address) {
        clearActivity();
        statistics.connect(address);
        refreshStatistics();
    }

    public void disconnect() {
        statistics.disconnect();
        clearActivity();
        refreshStatistics();
    }

    @Override
    public void afterBlockBreak(@NonNull ClientLevel world, @NonNull LocalPlayer playerEntity, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        if (!statistics.isConnected()) return;
        long now = System.currentTimeMillis();
        if(lastBlockBreakAt < 0 || now - lastBlockBreakAt > 2_000) {
            blocksAwaitingPrice = 0;
            MiningGoals.getInstance().expirePendingPrices();
        }
        record(1, 0, 0);
        lastBlockBreakAt = now;
        paused = false;
        AdditionalGoals.getInstance().blockBroken(!(lastActionBarAt >= 0 && now - lastActionBarAt <= MAX_LATEST_ACTION_BAR_MS));
        boolean pricePending = addMoney(now);
        MiningGoals.getInstance().blockBroken(pricePending);
    }
    public boolean hasStatistics() {
        return totalBrokenBlocks != 0 || uptime != 0 || totalMoney != 0 || totalShards != 0;
    }

    public void reset() {
        statistics.reset();
        clearActivity();
        refreshStatistics();
    }

    private void clearActivity() {
        lastBlockBreakAt = -1;
        lastActionBarAt = -1;
        blocksAwaitingPrice = 0;
        latestActionBar = 0;
        latestActionBarTimeout = 100;
        paused = true;
    }
    public void updateActionBar(Component text) {
        if (!statistics.isConnected()) return;
        Matcher matchingText = actionBarPattern.matcher(text.getString().replaceAll("§.", ""));
        if(matchingText.find()) {

            long price = MoneyUtils.convertFrom(matchingText.group(1));
            if(price / 10 > latestActionBar && latestActionBarTimeout < 10) {
                latestActionBarTimeout++;
            } else {
                latestActionBar = price;
                latestActionBarTimeout = 0;
                long now = System.currentTimeMillis();
                lastActionBarAt = now;
                if(blocksAwaitingPrice > 0 && lastBlockBreakAt >= 0 && now - lastBlockBreakAt <= 2_000) {
                    long earned = price > Long.MAX_VALUE / blocksAwaitingPrice
                            ? Long.MAX_VALUE : price * blocksAwaitingPrice;
                    if (Arrays.asList(ConfigMining.bphWidgetAllowed).contains(ConfigMining.bphAllowEnum.BLOCKS)) {
                        record(0, earned, 0);
                    }
                    MiningGoals.getInstance().priceForPendingBlocks(price);
                }
                blocksAwaitingPrice = 0;
            }
        }
    }

    public void getMessage(Component text, boolean overlay) {
        if(overlay || !statistics.isConnected()) return;
        String msg = text.getString();

        if(msg.startsWith("Вы нашли шард!")) {

            latestActionBarTimeout = 0;
            if (msg.length() > 14) {
                Matcher matcher = shardMultiplierPattern.matcher(msg);
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
            Matcher matchingText = moneyPattern.matcher(text.getString().replaceAll("§.", ""));
            if(matchingText.find()) {
                long price = MoneyUtils.convertFrom(matchingText.group(1));
                record(0, price, 0);
                MiningGoals.getInstance().moneyEarned(price);

                latestActionBarTimeout = 0;
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

    private boolean addMoney(long now) {
        if(lastActionBarAt >= 0 && now - lastActionBarAt <= MAX_LATEST_ACTION_BAR_MS) {
            if (Arrays.asList(ConfigMining.bphWidgetAllowed).contains(ConfigMining.bphAllowEnum.BLOCKS)) {
                record(0, latestActionBar, 0);
            }
            MiningGoals.getInstance().moneyEarned(latestActionBar);
            return false;
        } else {
            blocksAwaitingPrice++;
            return true;
        }
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
