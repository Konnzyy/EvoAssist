/*
 * Modified for EvoAssist by Konnzyy in 2026.
 * Licensed under the Apache License 2.0.
 */
package com.konzy.evo_assist.client.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatTabManager {
    private static ChatTabManager instance;
    private ChatPrefix currentPrefix = ChatPrefix.NONE;
    private final List<ChatTab> bannedTabs = new ArrayList<>();

    private final List<ChatTab> tabs = new ArrayList<>();
    private ChatTab activeTab;

    private ChatTabManager() {
        createTabs();
        activeTab = tabs.getFirst();
    }

    public static ChatTabManager getInstance() {
        if (instance == null) instance = new ChatTabManager();
        return instance;
    }

    private void createTabs() {
        tabs.add(new ChatTab("evoassist.chat.all", Collections.emptyList(),
                0xFFFFFF, 0xFFFF55, 0xAAAAAA, 0xFF5555));

        tabs.add(new ChatTab("evoassist.chat.clan", List.of("[Клан]"),
                0xFFFFFF, 0xFFFF55, 0xAAAAAA, 0xFF5555));

        tabs.add(new ChatTab("evoassist.chat.private", List.of("ЛС | "),
                0xFFFFFF, 0xFFFF55, 0xAAAAAA, 0xFF5555));

        tabs.add(new ChatTab("L", List.of("Ⓛ "),
                0xFFFFFF, 0xFFFF55, 0xAAAAAA, 0xFF5555));
        tabs.add(new ChatTab("G", List.of("Ⓖ "),
                0xFFFFFF, 0xFFFF55, 0xAAAAAA, 0xFF5555));

        tabs.add(new ChatTab("M", List.of("Ⓜ "),
                0xFFFFFF, 0xFFFF55, 0xAAAAAA, 0xFF5555));
    }

    public boolean shouldDisplayMessage(Component message) {
        for(ChatTab banned : bannedTabs) {
            if(banned != activeTab && banned.matches(message)) {
                return false;
            }
        }
        return activeTab != null && activeTab.matches(message);
    }

    public void setActiveTab(ChatTab tab) {
        if (tab != null && tab != this.activeTab) {
            this.activeTab = tab;
            Minecraft.getInstance().gui.hud.getChat().rescaleChat();
        }
    }
    public ChatTab getActiveTab() { return this.activeTab; }
    public boolean isBanned(ChatTab tab) {
        if (tab != null) {
            return this.bannedTabs.contains(tab);
        }
        return false;
    }
    public void setBannedTab(ChatTab tab) {
        if (tab != null) {
            if(this.bannedTabs.contains(tab)) {
                this.bannedTabs.remove(tab);
            } else this.bannedTabs.add(tab);

            Minecraft.getInstance().gui.hud.getChat().rescaleChat();
        }
    }
    public List<ChatTab> getTabs() { return this.tabs; }

    public ChatPrefix getCurrentPrefix() { return this.currentPrefix; }
    public void setPrefix(ChatPrefix prefix) { this.currentPrefix = prefix; }
}
