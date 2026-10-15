package com.konzy.evo_assist.client.mixin;

import com.konzy.evo_assist.client.features.mine.blockPH.BlockProfitPerHour;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe each incoming reward once, before HUD display filters can replace or hide it. */
@Mixin(ClientPacketListener.class)
public class MiningRewardPacketMixin {
    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void serverBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo info) {
        if (!Minecraft.getInstance().isSameThread()) return;
        var counter = BlockProfitPerHour.getInstance();
        if (counter != null) counter.serverBlockUpdate(packet.getPos(), packet.getBlockState());
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"))
    private void serverSectionBlockUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo info) {
        if (!Minecraft.getInstance().isSameThread()) return;
        var counter = BlockProfitPerHour.getInstance();
        if (counter != null) packet.runUpdates((pos, state) -> counter.serverBlockUpdate(pos, state));
    }

    @Inject(method = "setActionBarText", at = @At("HEAD"))
    private void actionBar(ClientboundSetActionBarTextPacket packet, CallbackInfo info) {
        receive(packet.text());
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void overlayChat(ClientboundSystemChatPacket packet, CallbackInfo info) {
        if (packet.overlay()) receive(packet.content());
    }

    private static void receive(Component message) {
        // Vanilla may enter the handler on the network thread before scheduling it on the client.
        if (!Minecraft.getInstance().isSameThread()) return;
        var counter = BlockProfitPerHour.getInstance();
        if (counter != null) counter.updateActionBar(message);
    }
}
