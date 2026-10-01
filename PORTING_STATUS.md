# Evo Extras 26.2 port: stages 1–4

## Completed

1. Saved an unchanged 1.21.4 source snapshot in `../Evo-Extras-1.21.4-baseline` with `BASELINE-SHA256.csv` and a feature/configuration inventory. The original `D:\Evo-Extras` was not edited.
2. Updated the working copy to Minecraft 26.2, Gradle 9.5.1, Fabric Loom 1.17.21, Loader 0.19.5, Fabric API 0.158.0+26.2, Java 25, and Resourceful Config 5.0.0.
3. Migrated Yarn names to Mojang mappings; updated GUI, HUD, key mapping, client commands and the three retained client mixins for 26.2. Replaced the old chat HUD mixin with Minecraft's visible message filter.
4. Built `EvoExtras-1.5.2+mc26.2.jar`. `gradlew build` succeeds. A `gradlew runClient` smoke test initialized Evo Extras and loaded client resources without a mod/mixin loading error.

## UI update (1.5.3)

- Added a settings screen with a persistent left sidebar, scrollable option cards and direct access to advanced Resourceful Config options.
- The autoclicker page now shows its current CPS, the full 1–20 CPS range and a key capture button. The key is saved to Minecraft controls.
- Replaced window opacity with `widgetOpacity`, which adjusts the background, text, progress bars and icons of Evo Extras HUD widgets only. The former window-opacity fields are no longer used.
- `gradlew build` succeeds for `EvoExtras-1.5.3+mc26.2.jar`. The new screen still needs a visual and interaction check in the game.

## UI refinement (1.5.4)

- Made the settings panel smaller and lighter. The Visual page has separate opacity controls for the settings menu and Evo Extras HUD widgets; text remains opaque for readability.
- Added an in-menu widget editor button, the mod version, default values and reset buttons for numeric/color settings.
- Reworked autoclicker activation: Press mode repeats clicks at the selected CPS; Hold mode latches the chosen mouse button after one activation-key press and releases it on the next. The mining-source selector allows multiple choices.
- The mining-counter reset action now runs inside the settings screen and leaves it open.
- `gradlew build` succeeds for `EvoExtras-1.5.4+mc26.2.jar`. The new interactions still need an in-game check.

## Combobox and settings access update (1.5.5)

- Replaced expanding option cards with compact popup comboboxes. Mining sources use checkmarks and support multiple selections without closing the popup or moving other settings.
- Removed default-value captions from option descriptions; reset buttons retain their defaults.
- Removed the autoclicker enable row. Only its activation key can start it during gameplay. Opening a GUI, leaving the world or dying stops the clicker and releases any held button.
- Added a dedicated optional Mod Menu entrypoint that opens the same settings screen from the main menu. Mod Menu 20.0.3 is a development/compile dependency and is not embedded in the release JAR.
- `gradlew build` succeeds for `EvoExtras-1.5.5+mc26.2.jar`. Visual interaction still needs an in-game check.

## Test boundary

Version 1.5.8 adds a persistent autoclicker enable switch that blocks its key binding when off and releases an active held mouse button. The switch does not start clicking inside the menu. Rune duration and boss sound notification features, their menu sections, HUD widget, config categories and sound asset were removed to avoid duplicating EvoPlus. The settings header now reads `EvoExtras`. The remaining menu sections are Autoclicker, Mining, Chat and Visual.

Version 1.5.7 places the autoclicker range endpoints (`1` and `20`) beside the CPS slider and removes `CPS` from the endpoint labels. The numeric value box continues to show the selected CPS.

Version 1.5.6 resets the mining counter on client connection and disconnection events, including reconnecting to the same server. The reset also clears the cached action-bar price so earnings from a previous session cannot leak into the new one. `gradlew build` succeeds; reconnect behavior still needs an in-game check.


The client smoke test did not join the DiamondWorld server. Chat tabs/prefixes, block-profit parsing, local booster tracking, autoclicker behavior, and widget appearance still need an in-game/server check. Existing 1.21.4 user configuration was not supplied for a migration test; back it up before using the 26.2 build.

## Build

Use Java 25 and run `./gradlew build`. The installable JAR is in `build/libs`; the `-sources.jar` is source code only. Fabric API must be installed alongside this mod. Resourceful Config is embedded in the JAR.
