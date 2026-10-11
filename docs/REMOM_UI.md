# Remom UI on CS engine — what “full replace” means

## Already done on this repo
- `app_name` / build types → **RemomLauncher** (`app_pojavlauncher/build.gradle`)
- Blue dark theme → `colors_cs.xml`
- README → RemomLauncher

## What cannot be one-click copy-paste
This app is **hundreds of XML layouts + Java activities** (Pojav/CS UI).
Deleting “all CS features” and drawing a brand-new UI means:
1. New layouts for home, accounts, profiles, mods, settings, about
2. Wiring every button to existing engine APIs (launch, download, accounts)
3. Testing on device after each Gradle build

That is **days/weeks of work**, not one commit.

## Fast path (what we do)
1. **Brand** every visible CS/Nexus string → Remom (strings.xml)
2. **Theme** blue (colors already started)
3. **About** page text → Remom + your GitHub
4. Keep **engine features** (play, mods, controls, offline) — these ARE the product
5. Only remove/hide screens you truly don’t want (Firebase promo, etc.) one by one

## strings.xml Remom branding
Apply on a machine with the repo:

```bash
cd CS-LAUNCHER-v3
sed -i \
  -e 's/Nexus Launcher CS/RemomLauncher/g' \
  -e 's/Nexus Launcher/RemomLauncher/g' \
  -e 's/CS Launcher V3\.1/RemomLauncher/g' \
  -e 's/CS Launcher V3/RemomLauncher/g' \
  -e 's/CS Launcher/RemomLauncher/g' \
  app_pojavlauncher/src/main/res/values/strings.xml
```

Then commit. (Large file; same result as full replace of brand names.)

## Build APK
```bash
./gradlew :app_pojavlauncher:assembleDebug
```

## Next commits (order)
1. Finish strings brand + about links
2. Hide/remove unwanted menu entries if any
3. Icon / splash Remom
4. Optional: new home layout only (biggest visual change)
