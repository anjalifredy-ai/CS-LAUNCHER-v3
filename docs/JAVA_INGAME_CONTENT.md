# In-game content (Java)

## Already in Remom / CS path

While Minecraft is running, open the **in-game menu** (drawer / controls menu) → resource browser entry.

Tabs:

| Tab | Folder | Apply |
|-----|--------|--------|
| PACKS | resourcepacks/ | Often in-game (options → resource packs) |
| SHADERS | shaderpacks/ | Usually needs reload |
| **MODS** | mods/ | **Full game restart required** |

Modrinth API filters by current profile MC version when known.

## Why not seamless back to the same block

- Fabric/Forge load mods at **startup** only
- True hot-swap without Mojang loading screen is not supported on mobile loaders
- After mod install: force-close → Launch same profile → open world again
- `GameSessionRestore` can remember last world/server **name** for a tip (does not auto-inject into MC)

## Flow

1. Game running
2. Menu → Browser → Mods / Packs
3. Install from Modrinth
4. Mods: restart game (loading screen)
5. Manually join same world
