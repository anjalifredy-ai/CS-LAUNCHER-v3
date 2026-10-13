# Current step

**DONE:** `docs/NATIVE_LOAD_BARRIERS.md` + `NativeLoadProbe`
- Tries `System.load` on extracted .so (owned APK path only)
- Logs success/fail to `meta/load_probe.json`
- Does **not** start Minecraft / game window

Checklist can show probe attempted; `gameWindow` stays false.

**Next:** stop claiming more “play” progress until real host design; or Java launcher polish.

Say **"agla step"**.
