# Home hero photo — upload once

1. Download `remom_hero_banner.png` (or `.jpg`) from chat / artifacts.
2. Open this folder on GitHub:
   https://github.com/anjalifredy-ai/CS-LAUNCHER-v3/upload/main/app_pojavlauncher/src/main/res/drawable-nodpi
3. Upload file named exactly:
   `remom_hero_banner.png`
   (or `remom_hero_banner.jpg` — then change layout src to `@drawable/remom_hero_banner` still works for both if same name without extension conflict; prefer **png**)
4. Commit → rebuild APK.

Layout already uses `@drawable/remom_hero_banner` on home hero.
Suggested size: **1920×600** landscape (already cropped for you).
