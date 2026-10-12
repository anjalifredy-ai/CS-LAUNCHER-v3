# Bedrock: owned verify → multi-version profiles

## What you described (target)

1. User installs **official** Minecraft from Play Store  
2. Remom **verifies** package present (owned/install gate)  
3. Minecraft stays on phone  
4. User can **switch versions** (old / new) via **Bedrock profiles**  
5. Play → that version runs; worlds / servers work per isolation  

## What works today (Phase 1)

| Step | Status |
|------|--------|
| Java / Bedrock header switch | Yes (`edition_spinner`) |
| Detect `com.mojang.minecraftpe` installed | Yes (`EditionMode.isOfficialBedrockInstalled`) |
| No install → block + Play Store | Yes (`tryLaunchBedrock`) |
| Launch official current install | Yes |
| Multiple isolated Bedrock versions + profiles | **Not yet** (Phase 2–3) |

Android **cannot** fully prove Microsoft purchase from a third-party app.  
Practical gate = **official package installed** (same as LeviLaunchroid-style launchers).  
Final license is still enforced by **Mojang’s own client** when it runs.

## Phase 2 — Bedrock profiles (UI + data)

Mirror Java profiles:

```
bedrock_profiles/
  <profileId>/
    name.json          # display name, target version id
    isolation_path     # data dir for worlds/packs
```

UI:
- List Bedrock profiles  
- Create profile → pick version label  
- Select profile → Play uses that isolation path  

## Phase 3 — Multi-version runtime (hard)

This is **LeviLaunchroid-class** work:

- Import / manage **official** version builds (not cracked APKs)  
- Version isolation (separate worlds, resource packs, behavior packs)  
- Launch correct native binaries for that version  
- Survive every Mojang update  
- Servers / Realms still depend on that version’s official client behavior  

Until Phase 3 is built, **Play in Bedrock mode** opens the **currently installed** official Minecraft (one active Play Store install).

## Honest limit

“Sare versions switch + worlds + servers sab working” = months of native/Android packaging work, not a single UI commit.  
Phase 1 gate is correct foundation: **no official Minecraft → no Bedrock.**

## Wire Play (Java path)

```java
if (EditionMode.tryLaunchBedrock(this)) return;
// else existing Java launch…
```
