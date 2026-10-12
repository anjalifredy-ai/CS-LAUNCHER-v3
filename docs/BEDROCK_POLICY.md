# Bedrock in Remom — policy

## Goal you asked for
- APK / versions management for Bedrock
- **No own Minecraft → game does not open**
- Feel like Bedrock is part of the launcher

## What is realistic and legal

| Piece | Reality |
|-------|--------|
| Launcher UI (Java/Bedrock switch, lists, settings) | Inside Remom |
| Bedrock **game binary** | Only **official** Mojang app |
| Play without purchase / cracked APK | **Blocked** (policy) |
| Multi-version isolated APK sandbox | LeviLaunchroid-class project (large) |

Remom **does not** reimplement Bedrock graphics/network from scratch.

## Current implementation

`EditionMode`:
1. Mode = Bedrock
2. If `com.mojang.minecraftpe` **not** installed → dialog: buy/install on Play Store, **no launch**
3. If installed → start official app

Wire on Play:
```java
if (EditionMode.tryLaunchBedrock(this)) return;
```

## “Sare versions APK import”

That needs:
- Official APKs only (user’s licensed installs / exports allowed by platform)
- Version isolation directories
- Native load / patch pipeline (like Levi)
- Continuous breaks on every Mojang update

**Phase 1 (now):** ownership gate + official launch  
**Phase 2 (later):** optional official multi-version manager module — not cracked clients

## User rule (your words)
> agar kisi ke pass minecraft owned nhi hai toh game open nhi hoga

Enforced by: no official package → no Bedrock start.
