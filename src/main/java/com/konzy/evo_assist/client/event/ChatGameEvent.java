/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.event;

import com.konzy.evo_assist.client.EvoAssistClient;
import com.konzy.evo_assist.client.features.mine.blockPH.BlockProfitPerHour;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class ChatGameEvent implements ClientReceiveMessageEvents.Game {
    @Override
    public void onReceiveGameMessage(@NonNull Component text, boolean b) {
        BlockProfitPerHour.getInstance().getMessage(text, b);
        if (!b) EvoAssistClient.rewardStatistics.receive(text.getString());
    }
}
