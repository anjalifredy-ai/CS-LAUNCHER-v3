# Bedrock host layer (Levi-style direction)

## Architecture

```
Play button
   → BedrockHostRouter
        → OfficialPlayStoreHost   (works now)
        → StubNativeHost          (imported APK; inventory only)
        → [Future] NativeHost     (load .so + run)
```

## Done
- Isolation paths, profiles, import, validate
- Host interface + router
- APK native **inventory** (list .so names by ABI) — no load

## Not done
- Extract + `System.load` / linker for Bedrock engine
- In-process game window
- Multi-version play

## Rules
- No mirror APK download
- Only user-imported `com.mojang.minecraftpe`
- Ownership gate: Play package installed
