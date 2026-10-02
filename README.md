# EvoAssist for Minecraft 26.2

Client-side Fabric mod for DiamondWorld Prison Evo.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.5 or newer
- Fabric API for Minecraft 26.2
- Java 25

Resourceful Config 5.0.0 is bundled in the mod JAR.

## Installation

Copy `EvoAssist-v1.0.0+mc26.2.jar` and Fabric API into the game's `mods` folder. Start Minecraft with the Fabric 26.2 profile.

Press Insert to open the settings, or use Mod Menu. You can assign a different menu key under Interface > Menu settings, using the same binding as Minecraft's Controls screen. The sections are Autoclicker, Bosses, Mine, Mining Goals, Clan, Clan Goals and Interface. Russian and English follow Minecraft's selected language automatically; other languages use the English fallback.

The autoclicker starts through its assigned key during gameplay and stops when a screen opens. Click mode generates repeated clicks at the selected CPS; Hold mode holds the mouse button until the key is pressed again. Commands: `/evoassist` or `/ea` for settings; `/evoassistwidgets` or `/eaw` for the widget editor. Legacy command aliases remain supported.

The plus beside the mod title opens the editor for all widgets. While a world is loaded, the editor leaves the game scene sharp and undimmed, allowing other mods' visible HUD elements to be used as positioning references. Other mods may independently hide their widgets when a screen is open. HUD widgets have no background during gameplay; their editor backgrounds show the clickable bounds.

Mining Goals has independent block, active time, money and shard goals. Clan Goals has separate points, gold and experience goals. Completed goals freeze until reset or replaced. Amount targets support decimal abbreviations such as `5.32B` or `5.32K`. Money and shard targets are limited to `999Q`, clan targets to `1M`, and time targets to 9999 minutes. These limits are enforced without being listed in the input hints. Completion notification duration is shared across goal types and adjustable from 1 to 60 seconds. Goal statistics and widget positions survive reconnects and restarts.

Mining totals are saved separately for each server and survive reconnects and restarts until you reset them in the mod menu. The per-hour estimate uses recent mining activity and gradually falls to zero when you stop mining.

Bosses has one widget for money, shards and tokens from boss/dungeon reward messages. Clan has one widget for clan points, experience and gold. Each section has a display switch, an independent statistics reset and a button to edit that widget's position and scale. Hiding the widget does not stop counting. Money uses K, M, B, T and Q with up to two decimals; other rewards use whole numbers. Rewards are saved immediately in `config/evoassist_rewards.properties`, separately for each server address, and survive reconnects and game restarts until manually reset. The parser counts the displayed award, including its bonus; it recognizes server system reward lines starting with +, not player chat or action-bar mining income. Identical system reward lines from other activities would also be included.

The Fabric mod ID is `evoassist`. Configuration is saved in `config/evoassist.jsonc`; mining goals use `config/evoassist_mining_goals.properties`. This ID change starts with fresh settings and statistics rather than loading files created under `evo_assist`.

## Building

Run `./gradlew build` with Java 25. The mod JAR is generated in `build/libs` (use the file without `-sources`).

The build runs checks for reward parsing, compact currency formatting, goal limits and persistence, server separation, chat layout bounds, and localized time formatting without launching Minecraft. Both language catalogs are stored under `src/main/resources/assets/evoassist/lang`.

## License

Copyright 2026 Konnzy. Original EvoAssist contributions and modifications are offered under the MIT License in `LICENSE.txt`.