# _2ndy Client (Fabric, Minecraft 1.21.11)

- **Right Shift** — module menu (3 columns: categories | module cards with search | settings of the selected module). Just start typing to search, filter by *All / Enabled*, click a card to toggle it, **Settings** or right-click to open its settings on the right. The crosshair is drawn right inside the panel.
- **Chat (T)** — move HUD elements with the mouse; **drag the small square in an element's bottom-right corner to resize it**; right-click an element to reset its size
- **Zoom** — hold the key (default C); rebind it in the module settings (click the *Key* row, press any key or mouse button; Esc cancels, Backspace resets). Zoom FOV is a slider
- **Color** tab in the menu — any interface color (R/G/B or presets)
- Every HUD module has a **BG** switch (transparent background)
- Modules with a **Settings** button have their own page: sliders, switches, mode pickers, colors (R/G/B, presets, rainbow)
- **Custom Crosshair** (Visual) — built-in styles (cross, dot, cross + dot, square) or **draw your own**: set *Style* to *Custom (drawn)* and press **Edit** next to *Custom design*. Left-click paints, right-click erases, with a color palette, mirror mode, presets and a live preview. Also: pixel size, thickness, gap, opacity, outline, color on target
- **Target HUD**, **Attack Cooldown**, **Kill Counter** (PvP), **Death Point** (HUD) — all configurable
- **Freelook** (Visual) — hold **Left Alt** (rebindable) to look around without turning the player; optional third-person view and sensitivity
- **Block Overlay** (Visual) — own outline for the targeted block: color, opacity, thickness (2D overlay, first person; the vanilla outline stays underneath)
- **Low Fire** / **Low Shield** (Visual) — fire overlay and shield sit lower; adjustable
- **Item Customizer** (Visual) — position, rotation and size of held items. *All items* profile + separate profiles per item: hold an item and press *Add held item*, then edit with the sliders (*Profile* row switches between them)
- **Damage Indicator** (PvP) — floating damage numbers over targets (only your hits or all, health or hearts)
- **Item Tooltip+** (Utility) — durability, stack size and item id in tooltips
- **Pearl Trajectory** (Visual) — shows where the held ender pearl will land (2D overlay, first person, blocks only)
- **Hit Sound** (PvP) — hit sound: a built-in one or **your own file**. Put `.wav` / `.ogg` files into `.minecraft/config/secondy_client/sounds/` (button *Open sounds folder* in the module settings), choose *Source: Custom file* and click the *File* row to preview. MP3 is not supported — convert it to WAV or OGG. Sounds longer than 5 seconds are cut off.
- **Armor Break Alert** (PvP) — warns when armor durability drops below a threshold (on-screen text + sound)
- **Presets** (Utility) — 5 slots: save / load / clear all module settings. Files are stored next to the config: `secondy_client_preset_N.json`
- **Optimizer** (Optimize tab) — more FPS through the game's own settings. Every option is toggled separately: FPS limit in a minimized window (on by default, does not touch visuals), uncap FPS, disable VSync, entity shadows, particles, entity distance, render distance limit, biome blend. Three ready-made profiles: safe / balanced / max. Your original settings are remembered and restored when the module is turned off and when you quit (backup in `config/secondy_client_optimizer_backup.json` in case of a crash). While the module is on it keeps the chosen values: if you change the same settings in the game's options, turn the matching option off first.
- **Tall grass** is removed by the resource pack `_2ndy_no_grass_pack.zip` (put it into `resourcepacks/` and enable it), not by a module.
- More modules: Day Counter, Playtime, Health, XP Display, Held Item, Dimension, Fall Distance, Pot Counter, Pearl Counter, Arrow Counter (off by default)
- **Hit Color** lives in the `optional-hitcolor/` folder (see INSTALL.txt) — add it after the main build passes

Build: **Actions** tab on GitHub → latest `build` run → **Artifacts** → `_2ndy-pvp-client` (contains `_2ndy-pvp-client.jar`).
Locally: `gradle build` (Gradle 9.2.1, Java 21), the jar ends up in `build/libs/`.
