package com.konzy.evo_assist.client.event;

import com.konzy.evo_assist.client.Evo_assistClient;
import com.konzy.evo_assist.client.features.mine.blockPH.BlockProfitPerHour;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

public class ChatGameEvent implements ClientReceiveMessageEvents.Game {
    @Override
    public void onReceiveGameMessage(Component text, boolean b) {
        BlockProfitPerHour.getInstance().getMessage(text, b);
        if (!b) Evo_assistClient.rewardStatistics.receive(text.getString());
    }
}
