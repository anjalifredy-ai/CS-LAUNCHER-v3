# Levi-style multi-version Bedrock play

## What you want
After owned/official Minecraft is verified → download/switch **all** Bedrock versions → Play works (worlds/servers).

## What LeviLaunchroid actually is
Open-source **separate** Android app ([LiteLDev/LeviLaunchroid](https://github.com/LiteLDev/LeviLaunchroid)):

- Requires **official** Google Play Minecraft (not a replacement for purchase)
- **Imports official APK** and runs **without** system reinstall of every build
- Multi-version + **version isolation**
- Native CMake / `.so` preloader / `:minecraft` module
- Continuous updates when Mojang ships new versions

That is a **full product**, not a Spinner + version list in Remom.

## What Remom is today
Remom = **Pojav / CS** stack → **Java Edition** in-process.

Bedrock side so far:

| Piece | Remom |
|-------|--------|
| Java/Bedrock UI switch | Yes |
| Official package gate | Yes |
| Profile + version **list** + isolation **folders** | Yes |
| Levi-class APK host + multi-version **play** | **No** |

Play in Bedrock mode still opens **one** official `com.mojang.minecraftpe` install.

## Legal ≠ finished engine
Owning Minecraft makes the **policy** legal.  
It does **not** invent the native host that extracts/runs alternate official builds.

Copying cracked APKs for “all versions” is still not OK.

## Real ways to get version play

### A) Recommended short term
Use **LeviLaunchroid** for Bedrock multi-version (owned game required).  
Keep **Remom** for Java (mods, Forge, Fabric).

### B) Long term product
1. Fork / study LeviLaunchroid under its license  
2. Either:
   - embed its `minecraft` launch path as a Remom module, or  
   - ship dual APK / deep-link Remom ↔ Levi  
3. Keep Play-Store authenticity checks on **every** launch path  
4. Months of native maintenance, not 2 days of UI  

### C) Not viable
“Version names in a RecyclerView = download + play all versions” — false.

## Bottom line
2 days of Remom work delivered **Java launcher UI + Bedrock gate + profile shell**.  
**Levi-level version play** = integrate or build that native project next — not a remaining checkbox in `EditionMode` alone.
