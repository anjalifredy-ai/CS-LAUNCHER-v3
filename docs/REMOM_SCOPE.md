# Remom scope (what stays / what changes)

## KEEP (user request: accounts + play)
- Offline / local accounts
- Microsoft accounts
- Ely.by (if present in base)
- Play / version download / worlds / multiplayer path
- Controls, RAM, renderer (needed to actually play)

## CHANGE to Remom
- App name, about, titles → **RemomLauncher**
- Colors → blue theme (`colors_cs.xml` done)
- CS/Nexus marketing text → Remom

## DO NOT delete wholesale
Removing “all CS UI features” without replacement layouts **breaks** the launcher.
UI screens call the engine. Empty shell = no play.

## Apply string branding (any PC / CI)
```bash
bash scripts/apply_remom_brand.sh
git add app_pojavlauncher/src/main/res/values/strings.xml
git commit -m "Remom: string branding"
```

## Animations / new UI
New animations and full custom home need new XML/Java and Gradle builds.
That is iterative, not a single paste.
