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
    @Override
    public void afterBlockBreak(@NonNull ClientLevel world, @NonNull LocalPlayer playerEntity, @NonNull BlockPos blockPos, @NonNull BlockState blockState) {
        long now = System.currentTimeMillis();
        if(lastBlockBreakAt < 0 || now - lastBlockBreakAt > 2_000) {
            blocksAwaitingPrice = 0;
            MiningGoals.getInstance().expirePendingPrices();
        }
        totalBrokenBlocks++;
        lastBlockBreakAt = now;
        paused = false;
        AdditionalGoals.getInstance().blockBroken(!(lastActionBarAt >= 0 && now - lastActionBarAt <= MAX_LATEST_ACTION_BAR_MS));
        boolean pricePending = addMoney(now);
        MiningGoals.getInstance().blockBroken(pricePending);
    }
    public void reset() {
        totalBrokenBlocks = 0;
        uptime = 0;
        totalMoney = 0;
        totalShards = 0;
        lastBlockBreakAt = -1;
        lastActionBarAt = -1;
        blocksAwaitingPrice = 0;
        latestActionBar = 0;
        latestActionBarTimeout = 100;
        paused = true;
        BlocksPerHour = 0;
        MoneyPerHour = 0;
        ShardsPerHour = 0;
    }
    public void updateActionBar(Component text) {
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
                    long earned = price * blocksAwaitingPrice;
                    if (Arrays.asList(ConfigMining.bphWidgetAllowed).contains(ConfigMining.bphAllowEnum.BLOCKS)) {
                        totalMoney += earned;
                    }
                    MiningGoals.getInstance().priceForPendingBlocks(price);
                }
                blocksAwaitingPrice = 0;
            }
        }
    }

    public void getMessage(Component text, boolean overlay) {
        if(overlay) return;
        String msg = text.getString();

        if(msg.startsWith("Вы нашли шард!")) {

            latestActionBarTimeout = 0;
            if (msg.length() > 14) {
                Matcher matcher = shardMultiplierPattern.matcher(msg);
                if (matcher.find(1)) {
                    int earned = Integer.parseInt(matcher.group(1));
                    totalShards += earned;
                    MiningGoals.getInstance().shardsEarned(earned);
                }
            } else {
                totalShards++;
                MiningGoals.getInstance().shardsEarned(1);
            }
        }


        boolean found = isFound(msg);
        if(found) {
            Matcher matchingText = moneyPattern.matcher(text.getString().replaceAll("§.", ""));
            if(matchingText.find()) {
                long price = MoneyUtils.convertFrom(matchingText.group(1));
                totalMoney += price;
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
                totalMoney += latestActionBar;
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
        if(paused) return;

        if(lastBlockBreakAt < 0 || System.currentTimeMillis() - lastBlockBreakAt >= AUTO_PAUSE_AFTER_MS) {
            paused = true;
            return;
        }
        uptime += 1000;
        MiningGoals.getInstance().activeSecond();

        BlocksPerHour = (long) Math.floor(((double) totalBrokenBlocks) / ((double) uptime / (1000 * 60 * 60)));
        MoneyPerHour = (long) Math.floor(((double) totalMoney) / ((double) uptime / (1000 * 60 * 60)));
        ShardsPerHour = (long) Math.floor(((double) totalShards) / ((double) uptime / (1000 * 60 * 60)));


    }


}
