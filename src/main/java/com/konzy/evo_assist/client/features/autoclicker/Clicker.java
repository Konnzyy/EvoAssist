package com.konzy.evo_assist.client.features.autoclicker;

import com.konzy.evo_assist.client.config.ConfigAutoclicker;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class Clicker {
    private static long lastClickTime;
    private static KeyMapping heldButton;

    public static void stop() {
        ConfigAutoclicker.autoclickerToggle = false;
        releaseHeldButton();
    }

    private static void releaseHeldButton() {
        if (heldButton != null) {
            heldButton.setDown(false);
            heldButton = null;
        }
    }

    public static void tick(Minecraft mc) {
        if (!ConfigAutoclicker.autoclickerEnabled || mc.level == null || mc.player == null
                || !mc.player.isAlive() || mc.gui.screen() != null) {
            stop();
            return;
        }
        KeyMapping target = ConfigAutoclicker.autoclickerButton == ConfigAutoclicker.ENUMAutoClickerButton.RMB
                ? mc.options.keyUse : mc.options.keyAttack;
        boolean available = ConfigAutoclicker.autoclickerToggle;
        boolean holdMode = ConfigAutoclicker.autoclickerActivation == ConfigAutoclicker.ENUMAutoClickerActivation.HOLD;

        if (!available || !holdMode || heldButton != target) {
            releaseHeldButton();
        }
        if (!available) return;

        if (holdMode) {
            if (heldButton == null) {
                heldButton = target;
                KeyMapping.click(InputConstants.getKey(target.saveString()));
            }
            heldButton.setDown(true);
            return;
        }

        long now = System.currentTimeMillis();
        int cps = Math.clamp(ConfigAutoclicker.autoclickerCps, 1, 20);
        if (now - lastClickTime >= 1000L / cps) {
            KeyMapping.click(InputConstants.getKey(target.saveString()));
            lastClickTime = now;
        }
    }
}
