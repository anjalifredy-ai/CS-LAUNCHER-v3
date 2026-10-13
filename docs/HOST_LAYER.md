# Bedrock host layer (Levi-style direction)

## Architecture

```
Play button
   → BedrockHostRouter
        → OfficialPlayStoreHost   (works now)
        → StubNativeHost          (imported APK; not executable yet)
        → [Future] NativeHost     (Levi-class)
```

## Rules
- Never download Bedrock APKs from mirrors
- Only user-imported official `com.mojang.minecraftpe`
- Ownership gate: Play Store package installed
- Router picks host by profile bind + APK presence

## What is NOT done
Loading `.so` from imported APK inside Remom process.
