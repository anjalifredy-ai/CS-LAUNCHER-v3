# Phase 3 — Bedrock multi-version (honest status)

## Done in this foundation

| Piece | Status |
|-------|--------|
| Version registry (prefs + labels) | ✅ `BedrockVersionManager` |
| Per-version isolation folders (worlds / packs) | ✅ under `files/bedrock_versions/<id>/` |
| Profile ↔ version binding (Phase 2 UI) | ✅ |
| Official package gate before Play | ✅ |
| APK Uri meta register (owned only) | ✅ pointer file, not cracked host |
| Native multi-version game host inside Remom | ❌ |
| Download every historical Bedrock build & run | ❌ |

## Why natives are hard

- One Play package name: `com.mojang.minecraftpe`
- Old versions need **Mojang binaries** + native load path
- LeviLaunchroid-class work: years, constant breakages on updates
- Remom will **not** ship cracked clients

## Play behavior today

1. Owned / official install detected  
2. Isolation dirs prepared for selected version id  
3. Launch = official current app  
4. When native host exists → launch selected isolation + matching binaries  

## Next engineering (real Phase 3 host)

1. Study open LeviLaunchroid launch pipeline (legal, GPL-aware)  
2. Official APK unpack → extract libs for **user-owned** builds only  
3. Isolated data path injection  
4. Test matrix per major version  

Until then UI lists versions; Play stays gated + official.
