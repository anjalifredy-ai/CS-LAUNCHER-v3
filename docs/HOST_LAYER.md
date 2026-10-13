# Bedrock host layer (Levi-style direction)

## Architecture

```
Play button
   → BedrockHostRouter
        → OfficialPlayStoreHost   (works — opens official app)
        → StubNativeHost          (import / extract / load probe)
        → [Future] full native host
```

## Pipeline so far
1. Import owned official APK
2. Validate package name
3. Inventory `lib/**/*.so`
4. Extract device ABI natives
5. Optional `System.load` **probe** (log only)

## Still missing for multi-version play
See `NATIVE_LOAD_BARRIERS.md`.

## Rules
- No mirror APK download
- Only user-imported `com.mojang.minecraftpe`
- Ownership gate: Play package installed
