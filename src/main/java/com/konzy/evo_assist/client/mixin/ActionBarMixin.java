package com.konzy.evo_assist.client.mixin;

import com.konzy.evo_assist.client.features.mine.blockPH.BlockProfitPerHour;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class ActionBarMixin {
    @Inject(at = @At("HEAD"), method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V")
    private void actionBar(Component message, boolean tinted, CallbackInfo info) {
        //Evo_extrasClient.logger.info("||||||||||||    " + message.toString());
        BlockProfitPerHour.getInstance().updateActionBar(message);
    }
}
