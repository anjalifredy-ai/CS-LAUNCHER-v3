# Remom Java FPS guide

Bedrock multi-version is separate. This is **Java Edition only** (Pojav path).

## Auto profile (`JavaFpsOptimizer`)

On first launch after update, Remom applies **Max FPS** defaults once:

| Setting | Value | Why |
|---------|--------|-----|
| `force_vsync` | **false** | Unlock FPS above screen refresh |
| `vsync_in_zink` | **true** | Zink follows game swapInterval (uncapped when MC vsync off) |
| `sustainedPerformance` | **false** | Prefer peak FPS over thermal lock |
| `allocation` | device-based | Enough heap, not so much GC chokes |
| `resolutionRatio` | smart scale | High-res panels scale toward 720/1080 |
| `javaArgs` | G1 + pause hints | Smoother GC on mobile (if empty) |

Re-apply anytime: clear pref `remom_java_fps_profile_v1` or call `JavaFpsOptimizer.applyMaxFpsProfile(ctx, true)`.

## In-game (Minecraft)

- Video Settings → **VSync OFF**, **Fullscreen OFF** if option exists
- Render distance moderate (6–10 mobile)
- Fancy → Fast, clouds off, particles decreased
- Sodium / Lithium / Starlight (Fabric) if you use mods — biggest real FPS gains

## Renderer

Settings → Renderer: try **MobileGlues / Zink / virgl** depending on GPU. One size does not fit all phones.

## Honest limit

Phone SoC + thermal + translation (GL) cap FPS. No launcher sets “infinite FPS” on every device.
