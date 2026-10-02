/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.mixin;

import com.konzy.evo_assist.client.chat.ChatPrefix;
import com.konzy.evo_assist.client.chat.ChatTabManager;
import com.konzy.evo_assist.client.config.ConfigChat;
import com.konzy.evo_assist.client.ui.widgets.WiChatTabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        if(!ConfigChat.chatTabsToggle) return;
        ChatTabManager manager = ChatTabManager.getInstance();
        addRenderableWidget(new WiChatTabs(null, prefix -> input.setValue(prefix.getPrefix())));
        ChatPrefix prefix = manager.getCurrentPrefix();

        if (input.getValue().isEmpty() && prefix != ChatPrefix.NONE) {
            input.setValue(prefix.getPrefix());
        }
    }

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String msg, boolean addToRecent, CallbackInfo ci) {
        if(!ConfigChat.chatTabsToggle) return;
        ChatPrefix prefix = ChatTabManager.getInstance().getCurrentPrefix();
        if (prefix != ChatPrefix.NONE) {
            String prefixStr = prefix.getPrefix();
            if (msg.equals(prefixStr)) {
                ci.cancel();
            }
        }
    }

}
