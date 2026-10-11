# RemomLauncher

**RemomLauncher** is a Minecraft: Java Edition launcher for Android, based on the CS Launcher V3 / Pojav lineage.

Engine (worlds, multiplayer, Forge/Fabric, controls) comes from this base.  
Branding and UI direction: **Remom** — landscape-first, blue theme over time.

---

## Already in this engine (from CS V3 base)

- Play Minecraft Java on Android (OpenJDK + renderers)
- Landscape-first / horizontal UI direction
- Offline profiles, Microsoft, Ely.by accounts
- Forge / Fabric / Quilt
- Mods, resource packs, shaders hub
- Custom controls + command buttons
- RAM / JVM / renderer settings

---

## Remom direction

| Goal | Status |
|------|--------|
| App name **RemomLauncher** | In progress (build.gradle) |
| Blue theme | Next |
| Horizontal polish | Base already landscape-first |
| Modrinth-style flow | Base has resource hub; extend |
| Keep engine intact | Yes — no rewrite |

---

## Build

```bash
git clone --recursive https://github.com/anjalifredy-ai/CS-LAUNCHER-v3.git
cd CS-LAUNCHER-v3
./gradlew :app_pojavlauncher:assembleDebug
```

APK: `app_pojavlauncher/build/outputs/apk/debug/`

Needs: Android SDK, NDK (see `app_pojavlauncher/build.gradle` ndkVersion), JDK.

Without a PC: use GitHub Actions if configured, or a friend’s PC once.

---

## License

GPL-3.0 (see LICENSE). Upstream Pojav / Boardwalk credits remain in NOTICE.

Not affiliated with Mojang or Microsoft. Play with accounts you are allowed to use.
