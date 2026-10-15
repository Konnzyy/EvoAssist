/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.mixin;

import com.konzy.evo_assist.client.chat.ChatPrefix;
import com.konzy.evo_assist.client.chat.ChatTabManager;
import com.konzy.evo_assist.client.compat.evoplus.EvoPlusIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.input.MouseButtonEvent;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Shadow
    protected EditBox input;


    protected ChatScreenMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        EvoPlusIntegration.invalidateChatHitArea();
        if(!EvoPlusIntegration.chatTabsEnabled()) return;
        ChatTabManager manager = ChatTabManager.getInstance();
        ChatPrefix prefix = manager.getCurrentPrefix();

        if (input.getValue().isEmpty() && prefix != ChatPrefix.NONE) {
            input.setValue(prefix.getPrefix());
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void evoassist$clickChatTab(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (EvoPlusIntegration.clickChatTabs((ChatScreen) (Object) this, event,
                prefix -> input.setValue(prefix.getPrefix()))) {
            setFocused(input);
            input.setFocused(true);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String msg, boolean addToRecent, CallbackInfo ci) {
        if(!EvoPlusIntegration.chatTabsEnabled()) return;
        ChatPrefix prefix = ChatTabManager.getInstance().getCurrentPrefix();
        if (prefix != ChatPrefix.NONE) {
            String prefixStr = prefix.getPrefix();
            if (msg.equals(prefixStr)) {
                ci.cancel();
            }
        }
    }

}
