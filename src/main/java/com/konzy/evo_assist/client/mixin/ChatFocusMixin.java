package com.konzy.evo_assist.client.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatFocusMixin extends Screen {
    @Shadow
    protected EditBox input;

    protected ChatFocusMixin(Component title) { super(title); }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void afterMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!input.isMouseOver(event.x(), event.y())) {
            this.setFocused(input);
            input.setFocused(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("RETURN"))
    private void afterKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (input != null && this.getFocused() != input) {
            this.setFocused(input);
            input.setFocused(true);
        }
    }
}
