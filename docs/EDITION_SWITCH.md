# Java / Bedrock edition switch (Remom)

## UI
Header dropdown: **Java** | **Bedrock** (`edition_spinner`).

## Java
Unchanged — runs inside Remom (Pojav/Amethyst path).

## Bedrock — honest facts

Minecraft **Bedrock** on Android is **closed source** (Mojang/Microsoft).
There is **no** open Bedrock engine like Java+LWJGL that you can rebuild in a weekend.

What real Bedrock launchers do (LeviLaunchroid, ModdedBE, Axion, etc.):

- Require **official** Minecraft from Google Play (`com.mojang.minecraftpe`)
- Import / patch / launch **that** APK or open the installed app

They do **not** invent a full Bedrock renderer from zero inside their process the way Pojav runs Java.

### Remom Bedrock mode (legal)

1. User selects **Bedrock** in the header
2. Play → opens official `com.mojang.minecraftpe` if installed
3. If not installed → Play Store page for Minecraft

**We will not** ship cracked / pirated Bedrock clients or reverse-engineered closed binaries.

## Wire-up

```java
// LauncherActivity after bindViews()
net.kdt.pojavlaunch.edition.EditionMode.bindHeader(this);

// On Play press, before Java launch:
if (net.kdt.pojavlaunch.edition.EditionMode.tryLaunchBedrock(this)) return;
```

Script: `scripts/apply_edition_mode.sh`
